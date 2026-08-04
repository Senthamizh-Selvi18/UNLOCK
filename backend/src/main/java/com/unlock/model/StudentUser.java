package com.unlock.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * One record per student who has ever logged in.
 * This is intentionally the SIMPLEST possible model - just enough
 * to prove "login -> save to database -> read it back" works end to end.
 * Evidence entries, reflections, and patterns come in later weeks.
 */
@Document(collection = "students")
public class StudentUser {

    @Id
    private String id;

    private String githubId;      // GitHub's unique numeric ID for this user
    private String username;      // GitHub login/handle, e.g. "octocat"
    private String displayName;   // GitHub "name" field, may be null
    private String avatarUrl;
    private String email;         // may be null if the student's GitHub email is private
    private Instant firstLoginAt;
    private Instant lastLoginAt;

    public StudentUser() {
    }

    // --- Getters and setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGithubId() {
        return githubId;
    }

    public void setGithubId(String githubId) {
        this.githubId = githubId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getFirstLoginAt() {
        return firstLoginAt;
    }

    public void setFirstLoginAt(Instant firstLoginAt) {
        this.firstLoginAt = firstLoginAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
