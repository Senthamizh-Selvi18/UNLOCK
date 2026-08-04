package com.unlock.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GitHub's API returns way more fields than we need per repo.
 * @JsonIgnoreProperties(ignoreUnknown = true) tells Jackson to just
 * ignore anything we didn't list here, instead of crashing.
 *
 * Full field reference: https://docs.github.com/en/rest/repos/repos
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubRepoDto {

    @JsonProperty("full_name")
    private String fullName;      // e.g. "octocat/hello-world"

    private String name;          // e.g. "hello-world"
    private String description;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("pushed_at")
    private String pushedAt;      // ISO date string of last activity

    @JsonProperty("created_at")
    private String createdAt;

    private boolean fork;         // we'll skip forked repos - they're not the student's own work

    // --- Getters and setters ---

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public String getPushedAt() {
        return pushedAt;
    }

    public void setPushedAt(String pushedAt) {
        this.pushedAt = pushedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isFork() {
        return fork;
    }

    public void setFork(boolean fork) {
        this.fork = fork;
    }
}
