package com.unlock.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * One fact in a student's timeline. Could come from GitHub automatically,
 * or be typed in manually for things software can't see.
 *
 * Deliberately has NO score fields anywhere - just dated facts with a
 * source and, where possible, a link to real proof.
 */
@Document(collection = "entries")
public class Entry {

    @Id
    private String id;

    private String studentId;       // links back to StudentUser.id
    private String title;
    private String description;
    private Instant date;           // when this actually happened, not when we saved it
    private EntrySource source;     // GITHUB_REPO or MANUAL
    private String externalId;      // e.g. GitHub repo full name - null for manual entries
    private String evidenceLink;    // link to the real proof (repo URL, uploaded file, etc.)
    private Instant createdAt;
    private Instant updatedAt;

    // Set by the student, not inferred. Distinguishes "I finished this and
    // moved on" from "I quietly abandoned this" - the Pattern Engine must
    // never call a completed project "stalled," since going quiet after
    // finishing something is the opposite of a concerning pattern.
    private boolean completed;

    public Entry() {
    }

    public enum EntrySource {
        GITHUB_REPO,
        MANUAL
    }

    // --- Getters and setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getDate() {
        return date;
    }

    public void setDate(Instant date) {
        this.date = date;
    }

    public EntrySource getSource() {
        return source;
    }

    public void setSource(EntrySource source) {
        this.source = source;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getEvidenceLink() {
        return evidenceLink;
    }

    public void setEvidenceLink(String evidenceLink) {
        this.evidenceLink = evidenceLink;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
