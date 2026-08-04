package com.unlock.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * One biweekly reflection: a question, and (once answered) the student's
 * own words. This is what makes UNLOCK worth opening regularly - not a
 * generic journal prompt, but a question grounded in what the student
 * said last time.
 */
@Document(collection = "reflections")
public class Reflection {

    @Id
    private String id;

    private String studentId;
    private String question;
    private String answer;       // null until the student responds
    private Instant createdAt;
    private Instant answeredAt;  // null until answered

    public Reflection() {
    }

    public boolean isAnswered() {
        return answer != null && !answer.isBlank();
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

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(Instant answeredAt) {
        this.answeredAt = answeredAt;
    }
}
