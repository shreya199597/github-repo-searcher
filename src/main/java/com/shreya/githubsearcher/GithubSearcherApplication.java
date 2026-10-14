package com.shreya.githubsearcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GithubSearcherApplication {

    public static void main(String[] args) {
        SpringApplication.run(GithubSearcherApplication.class, args);
    }
}
