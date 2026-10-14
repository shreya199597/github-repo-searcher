package com.shreya.githubsearcher.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
public class GitHubClientConfig {

    static final String API_VERSION = "2022-11-28";
    static final String USER_AGENT = "github-repo-searcher";

    @Bean
    public RestClient gitHubRestClient(RestClient.Builder builder, GitHubProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeaders(headers -> {
                    headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
                    headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
                    headers.set("X-GitHub-Api-Version", API_VERSION);
                    if (properties.hasToken()) {
                        headers.setBearerAuth(properties.token());
                    }
                })
                .build();
    }
}
