package com.unlock.dto;

import com.unlock.model.Entry;
import com.unlock.model.Pattern;
import com.unlock.model.Reflection;
import com.unlock.model.StudentUser;

import java.util.List;

/**
 * Everything needed to render a student's Growth Replay in one response.
 * Deliberately just re-packages real, already-verified data - nothing
 * new is invented here, this module only presents what already exists.
 */
public class GrowthReplayResponse {
    private StudentUser student;
    private List<Entry> entries;
    private List<Reflection> answeredReflections;
    private List<Pattern> confirmedPatterns;

    public GrowthReplayResponse(StudentUser student, List<Entry> entries,
                                 List<Reflection> answeredReflections,
                                 List<Pattern> confirmedPatterns) {
        this.student = student;
        this.entries = entries;
        this.answeredReflections = answeredReflections;
        this.confirmedPatterns = confirmedPatterns;
    }

    public StudentUser getStudent() {
        return student;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public List<Reflection> getAnsweredReflections() {
        return answeredReflections;
    }

    public List<Pattern> getConfirmedPatterns() {
        return confirmedPatterns;
    }
}
