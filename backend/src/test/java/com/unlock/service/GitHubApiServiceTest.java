package com.unlock.service;

import com.unlock.dto.GitHubRepoDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for GitHubApiService using Spring's MockRestServiceServer - this
 * intercepts the outgoing HTTP call and returns a scripted response, so
 * these tests never touch the real GitHub API. That matters here more
 * than almost anywhere else in the app: this is the one class that
 * talks to something outside our control, so it deserves to be pinned
 * down exactly, not just trusted by inspection.
 */
class GitHubApiServiceTest {

    @Test
    void fetchReposReturnsParsedReposFromARealLookingGithubResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        GitHubApiService service = new GitHubApiService(builder);

        String fakeGithubResponse = """
            [
              {
                "full_name": "octocat/hello-world",
                "name": "hello-world",
                "description": "My first project",
                "html_url": "https://github.com/octocat/hello-world",
                "pushed_at": "2026-01-10T12:00:00Z",
                "created_at": "2025-09-01T09:00:00Z",
                "fork": false
              }
            ]
            """;

        mockServer.expect(requestTo("https://api.github.com/user/repos?sort=pushed&per_page=30"))
                .andExpect(header("Authorization", "Bearer test-token-123"))
                .andRespond(withSuccess(fakeGithubResponse, MediaType.APPLICATION_JSON));

        List<GitHubRepoDto> repos = service.fetchRepos("test-token-123");

        mockServer.verify();
        assertEquals(1, repos.size());
        assertEquals("hello-world", repos.get(0).getName());
        assertEquals("octocat/hello-world", repos.get(0).getFullName());
        assertEquals("My first project", repos.get(0).getDescription());
    }

    @Test
    void fetchReposFiltersOutForkedRepos() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        GitHubApiService service = new GitHubApiService(builder);

        String fakeGithubResponse = """
            [
              { "full_name": "octocat/own-project", "name": "own-project", "fork": false, "pushed_at": "2026-01-10T12:00:00Z" },
              { "full_name": "octocat/forked-project", "name": "forked-project", "fork": true, "pushed_at": "2026-01-10T12:00:00Z" }
            ]
            """;

        mockServer.expect(requestTo("https://api.github.com/user/repos?sort=pushed&per_page=30"))
                .andRespond(withSuccess(fakeGithubResponse, MediaType.APPLICATION_JSON));

        List<GitHubRepoDto> repos = service.fetchRepos("test-token-123");

        assertEquals(1, repos.size(), "Forked repos must never be counted as the student's own evidence");
        assertEquals("own-project", repos.get(0).getName());
    }

    @Test
    void fetchReposReturnsEmptyListWhenGithubReturnsNoRepos() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        GitHubApiService service = new GitHubApiService(builder);

        mockServer.expect(requestTo("https://api.github.com/user/repos?sort=pushed&per_page=30"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<GitHubRepoDto> repos = service.fetchRepos("test-token-123");

        assertNotNull(repos);
        assertTrue(repos.isEmpty());
    }

    @Test
    void fetchReposIgnoresUnknownFieldsInsteadOfCrashing() {
        // GitHub's real API returns dozens of fields we don't care about.
        // GitHubRepoDto uses @JsonIgnoreProperties(ignoreUnknown = true) -
        // this test proves that actually works, not just that it compiles.
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        GitHubApiService service = new GitHubApiService(builder);

        String responseWithExtraFields = """
            [
              {
                "full_name": "octocat/hello-world",
                "name": "hello-world",
                "fork": false,
                "pushed_at": "2026-01-10T12:00:00Z",
                "stargazers_count": 42,
                "language": "Java",
                "owner": { "login": "octocat", "id": 1 },
                "topics": ["java", "spring"]
              }
            ]
            """;

        mockServer.expect(requestTo("https://api.github.com/user/repos?sort=pushed&per_page=30"))
                .andRespond(withSuccess(responseWithExtraFields, MediaType.APPLICATION_JSON));

        List<GitHubRepoDto> repos = service.fetchRepos("test-token-123");

        assertEquals(1, repos.size());
        assertEquals("hello-world", repos.get(0).getName());
    }
}
