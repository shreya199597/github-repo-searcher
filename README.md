# GitHub Repository Searcher

A Spring Boot REST API that searches GitHub repositories through the GitHub REST API, stores the results in PostgreSQL, and lets you query the stored results by language and star count.

- **Live API:** `<LIVE_URL>`
- **Swagger UI:** `<LIVE_URL>/swagger-ui/index.html` (the root URL redirects there)
- **OpenAPI JSON:** `<LIVE_URL>/v3/api-docs`
- **Postman collection:** [`postman/github-repo-searcher.postman_collection.json`](postman/github-repo-searcher.postman_collection.json)

> The live instance runs on a free tier that sleeps when idle, so the first request after a while can take 30–60 seconds.

## Tech stack

Java 21 · Spring Boot 3.5 (Web, Data JPA, Validation, Actuator) · PostgreSQL · Flyway · springdoc-openapi · JUnit 5, Mockito, MockMvc, MockRestServiceServer, embedded PostgreSQL · JaCoCo

## API

### 1. Search GitHub and save the results

`POST /api/github/search`

```json
{
  "query": "spring boot",
  "language": "Java",
  "sort": "stars"
}
```

| Field | Required | Rules |
|---|---|---|
| `query` | yes | Not blank, max 200 characters. Matched against repository **names** (partial or full match). |
| `language` | no | Max 50 characters; letters, digits, spaces and `. + # _ -` (so `C++`, `C#`, `Visual Basic .NET` work). |
| `sort` | no | `stars` (default), `forks` or `updated`, case-insensitive. Always descending. |

Response `200 OK`:

```json
{
  "message": "Repositories fetched and saved successfully",
  "repositories": [
    {
      "id": 123456,
      "name": "spring-boot-example",
      "description": "An example repository for Spring Boot",
      "owner": "user123",
      "language": "Java",
      "stars": 450,
      "forks": 120,
      "lastUpdated": "2024-01-01T12:00:00Z"
    }
  ]
}
```

If GitHub finds nothing, the response is still `200` with `"message": "No repositories found matching the search criteria"` and an empty list.

### 2. Get stored repositories

`GET /api/github/repositories?language=Java&minStars=100&sort=stars`

| Parameter | Default | Rules |
|---|---|---|
| `language` | — | Case-insensitive exact match (`java` matches `Java`, not `JavaScript`). |
| `minStars` | — | Integer ≥ 0, inclusive. |
| `sort` | `stars` | `stars`, `forks` or `updated`, descending. |
| `page` | `0` | Integer ≥ 0. *(addition to the spec)* |
| `size` | `50` | 1–100. *(addition to the spec)* |

Response `200 OK`: `{ "repositories": [ ... ] }` (same item shape as above).

### Errors

Every error uses the standard [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457) format (`application/problem+json`):

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "instance": "/api/github/search",
  "errors": [{ "field": "query", "message": "query must not be blank" }]
}
```

| Status | When |
|---|---|
| `400` | Invalid body or parameters, malformed JSON, unknown `sort`, or GitHub rejected the query (GitHub 422) |
| `415` | Body is not JSON |
| `429` | GitHub rate limit hit. Includes a `Retry-After` header and `retryAfterSeconds` / `resetAt` when GitHub provides them |
| `502` | GitHub returned an error or an unreadable response |
| `503` | GitHub could not be reached or timed out |
| `500` | Unexpected error (details are logged, not exposed) |

## Design

```
controller  →  service  →  client (GitHub REST API)
                       ↘  repository (PostgreSQL)
```

| Package | Responsibility |
|---|---|
| `controller` | HTTP layer: request validation, Swagger docs. No business logic. |
| `service` | `GitHubSearchService` (search, then save) and `RepositoryQueryService` (filtered reads). |
| `client` | `GitHubClient` calls GitHub and translates every failure into a domain exception. `GitHubQueryBuilder` builds the `q` string. |
| `repository` | Spring Data JPA for reads (`Specification`s for optional filters) plus a custom fragment for the upsert. |
| `entity` / `dto` / `client.dto` | Separate models for the database, our API and GitHub's API, so none of them leaks into the others. |
| `mapper` | Conversions between those models. |
| `exception` | Domain exceptions and the global `@RestControllerAdvice`. |

**Key decisions**

- **Upsert in one round trip.** `GitHubRepoUpsertRepositoryImpl` sends every result as one JDBC batch of PostgreSQL `INSERT ... ON CONFLICT (id) DO UPDATE`. That is one round trip per search rather than a SELECT plus an INSERT or UPDATE per repository. It is also race-free: two concurrent searches that return the same repository cannot create duplicates.
- **GitHub's id is the primary key.** The same repository always maps to the same row.
- **No transaction around the GitHub call.** The search service is deliberately not `@Transactional`, so a slow GitHub response never holds a database connection. Only the upsert runs in a transaction.
- **Name-only matching.** The query is sent as `<query> in:name`, which gives the "partial or full repository name match" the brief asks for. The language becomes a quoted `language:"..."` qualifier, and the whole `q` is passed as a URI variable so that characters like `+` are encoded (`C++` would otherwise reach GitHub as `C  `).
- **Rate limits.** GitHub signals rate limits with `429`, or with `403` plus either `x-ratelimit-remaining: 0` or `retry-after`. All of these become our `429` with a `Retry-After` header. Other `403`s become `502`.
- **Schema managed by Flyway.** Hibernate only validates the schema (`ddl-auto: validate`). Indexes back the language filter (`LOWER(language)`) and each sort column.
- **Timeouts.** 5 s connect and 10 s read on GitHub calls, so a hung connection cannot tie up request threads.

## Running locally

**Prerequisites:** JDK 21 and a PostgreSQL database. Maven is optional because the wrapper (`mvnw`) is included.

1. Provide a database, using either option:
   - Docker: `docker compose up -d` starts PostgreSQL on `localhost:5432`, matching the defaults.
   - Any PostgreSQL, e.g. a free [Neon](https://neon.tech) database: set the variables below.
2. Optionally create a GitHub token (no scopes needed). It raises the search limit from 10 to 30 requests per minute.
3. Run:

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

Then open http://localhost:8080 (it redirects to Swagger UI).

| Environment variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/github_searcher` | JDBC URL (for Neon add `?sslmode=require`) |
| `DB_USERNAME` | `postgres` | |
| `DB_PASSWORD` | `postgres` | |
| `GITHUB_TOKEN` | *(empty)* | Optional GitHub token |
| `GITHUB_PER_PAGE` | `30` | Results requested per search (max 100) |
| `PORT` | `8080` | HTTP port |

### Example requests

```bash
curl -X POST http://localhost:8080/api/github/search \
  -H "Content-Type: application/json" \
  -d '{"query": "spring boot", "language": "Java", "sort": "stars"}'

curl "http://localhost:8080/api/github/repositories?language=Java&minStars=100&sort=stars"
```

## Tests

```bash
./mvnw test
```

**No Docker needed.** Database tests run against a real PostgreSQL binary started in-process ([zonky embedded-postgres](https://github.com/zonkyio/embedded-postgres)), with the Flyway migrations applied. `ON CONFLICT` and the `LOWER()` index are PostgreSQL features, so testing on H2 would prove little.

| Test class | Kind | What it covers |
|---|---|---|
| `GitHubClientTest` | Unit, fake HTTP server | Request building and encoding; parsing; rate limits (403/429/secondary); 422; 5xx; timeouts; malformed and empty bodies |
| `GitHubQueryBuilderTest`, `SortFieldTest`, `RepositoryMapperTest` | Unit | Query string, sort parsing, mapping |
| `GitHubSearchServiceTest`, `RepositoryQueryServiceTest` | Unit, Mockito | Orchestration, empty results, invalid items, sorting and paging |
| `GitHubControllerTest` | `@WebMvcTest` | Response shape, validation and every error status |
| `GitHubRepoRepositoryIntegrationTest` | `@DataJpaTest`, PostgreSQL | Upsert inserts and updates without duplicates; filters; sorts; paging |
| `SearchFlowIntegrationTest` | `@SpringBootTest`, PostgreSQL | End to end: search twice → rows updated in place → read back with filters |

The coverage report is written to `target/site/jacoco/index.html` (about 99% of lines).

## Deployment

The app ships as a Docker image (`Dockerfile`, multi-stage, runs as a non-root user). The live instance runs on Render with a Neon PostgreSQL database, configured with the environment variables above. `/actuator/health` serves as the health check.

## Assumptions

- `query` is matched against repository names only, as the brief describes; GitHub's other qualifiers in the query text are passed through.
- Each search saves the first page of GitHub results (30 by default, configurable up to 100).
- `lastUpdated` is GitHub's `updated_at`. The database also stores `full_name`, watchers, open issues, URL, `created_at`, `pushed_at` and when we last fetched the row.
- The search endpoint returns `200`, matching the spec, rather than `201`: it is a search that also caches results, not the creation of one resource.
