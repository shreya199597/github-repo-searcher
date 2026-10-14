package com.shreya.githubsearcher.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gitHubSearcherOpenApi() {
        return new OpenAPI().info(new Info()
                .title("GitHub Repository Searcher API")
                .version("1.0.0")
                .description("""
                        Searches GitHub repositories through the GitHub REST API, stores the results \
                        in PostgreSQL (inserting new repositories and updating existing ones), and \
                        lets you query the stored results by language and star count."""));
    }
}
