package com.unlock.service;

import com.unlock.dto.GitHubRepoDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Talks to GitHub's real REST API - nothing here is guessed or made up.
 * If GitHub doesn't return it, we don't show it.
 *
 * Takes a RestClient.Builder via constructor injection (rather than
 * calling RestClient.create() internally) specifically so this class
 * can be tested with Spring's MockRestServiceServer, without ever
 * making a real network call during tests.
 */
@Service
public class GitHubApiService {

    private final RestClient restClient;

    public GitHubApiService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * Fetches the student's own repos (not forks), most recently active first.
     * Uses the access token GitHub gave us when the student logged in - the
     * student already approved this exact permission (read:user, user:email)
     * during login.
     */
    public List<GitHubRepoDto> fetchRepos(String accessToken) {
        String url = "https://api.github.com/user/repos?sort=pushed&per_page=30";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/vnd.github+json");

        ResponseEntity<GitHubRepoDto[]> response = restClient.get()
                .uri(url)
                .headers(h -> h.addAll(headers))
                .retrieve()
                .toEntity(GitHubRepoDto[].class);

        GitHubRepoDto[] body = response.getBody();
        if (body == null) {
            return List.of();
        }

        // Skip forked repos - those are copies of someone else's work,
        // not evidence of what the student themselves built.
        return List.of(body).stream()
                .filter(repo -> !repo.isFork())
                .toList();
    }
}
