package com.unlock.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * A pattern the system noticed - always backed by real evidence, never
 * shown as a verdict. The student can confirm it's accurate or reject it.
 *
 * confirmed = null   -> detected, student hasn't reviewed it yet
 * confirmed = true   -> student agrees this is accurate
 * confirmed = false  -> student says this isn't accurate (we stop showing it)
 */
@Document(collection = "patterns")
public class Pattern {

    @Id
    private String id;

    private String studentId;
    private String description;         // the observation, in friendly wording
    private List<String> evidenceEntryIds; // which Entry records led to this
    private Boolean confirmed;
    private String suggestion;           // optional, only shown after confirmation
    private boolean suggestionDismissed;
    private Instant createdAt;

    public Pattern() {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getEvidenceEntryIds() {
        return evidenceEntryIds;
    }

    public void setEvidenceEntryIds(List<String> evidenceEntryIds) {
        this.evidenceEntryIds = evidenceEntryIds;
    }

    public Boolean getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public boolean isSuggestionDismissed() {
        return suggestionDismissed;
    }

    public void setSuggestionDismissed(boolean suggestionDismissed) {
        this.suggestionDismissed = suggestionDismissed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
