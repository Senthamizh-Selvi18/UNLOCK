package com.unlock.service;

import com.unlock.model.Entry;
import com.unlock.model.Pattern;
import com.unlock.model.StudentUser;
import com.unlock.repository.EntryRepository;
import com.unlock.repository.PatternRepository;
import com.unlock.repository.StudentUserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * The Pattern Engine: looks across a student's real evidence and notices
 * things a single moment never would. Follows the honest design rules:
 *
 *   1. Never invent a fact - only use real Entry data.
 *   2. Never surface a "pattern" from a single data point - requires
 *      multiple instances, so one rough patch never gets labeled unfairly.
 *   3. Always attach the evidence that produced the observation.
 *   4. Always phrase it as an observation/question, never a verdict.
 */
@Service
public class PatternEngineService {

    // A repo with no activity for this many days is considered "stalled".
    static final int STALL_THRESHOLD_DAYS = 30;

    // Never call something a "pattern" from just one instance.
    static final int MIN_INSTANCES_FOR_PATTERN = 2;

    private final EntryRepository entryRepository;
    private final PatternRepository patternRepository;
    private final StudentUserRepository studentUserRepository;

    public PatternEngineService(EntryRepository entryRepository,
                                 PatternRepository patternRepository,
                                 StudentUserRepository studentUserRepository) {
        this.entryRepository = entryRepository;
        this.patternRepository = patternRepository;
        this.studentUserRepository = studentUserRepository;
    }

    /**
     * Scans a student's entries and creates new Pattern records if it
     * finds anything worth surfacing. Safe to call repeatedly - it won't
     * create duplicate patterns for the same underlying evidence.
     *
     * Returns the newly created patterns (empty list if nothing new found).
     */
    public List<Pattern> scanForPatterns(StudentUser student) {
        List<Pattern> newPatterns = new ArrayList<>();

        Pattern stalledPattern = detectStalledProjects(student);
        if (stalledPattern != null && !alreadyHasSimilarUnconfirmedPattern(student.getId(), stalledPattern)) {
            patternRepository.save(stalledPattern);
            newPatterns.add(stalledPattern);
        }

        return newPatterns;
    }

    /**
     * Runs automatically once a day and scans every student's evidence
     * for new patterns - mirrors how ReflectionService's scheduled job
     * works. The manual "/api/patterns/scan" endpoint still exists too,
     * both for instant feedback in the UI and for easier testing.
     */
    @Scheduled(cron = "0 30 8 * * *") // 8:30 AM daily, offset from the reflection job at 8:00
    public void scanAllStudentsAutomatically() {
        for (StudentUser student : studentUserRepository.findAll()) {
            scanForPatterns(student);
        }
    }

    /**
     * Looks for GitHub repos with no activity in a long time.
     * Only becomes a "pattern" if it happens more than once - a single
     * paused project proves nothing, several does.
     */
    private Pattern detectStalledProjects(StudentUser student) {
        List<Entry> entries = entryRepository.findByStudentIdOrderByDateDesc(student.getId());

        List<Entry> stalled = entries.stream()
                .filter(e -> e.getSource() == Entry.EntrySource.GITHUB_REPO)
                .filter(e -> !e.isCompleted()) // finished work is not the same as abandoned work
                .filter(e -> daysSince(e.getDate()) >= STALL_THRESHOLD_DAYS)
                .toList();

        if (stalled.size() < MIN_INSTANCES_FOR_PATTERN) {
            return null; // not enough evidence yet - stay quiet rather than guess
        }

        List<String> names = stalled.stream().map(Entry::getTitle).toList();
        List<String> evidenceIds = stalled.stream().map(Entry::getId).toList();

        String description = String.format(
                "%d of your projects haven't had activity in %d+ days: %s. " +
                "Just noticed the pattern - no judgment either way.",
                stalled.size(), STALL_THRESHOLD_DAYS, String.join(", ", names)
        );

        Pattern pattern = new Pattern();
        pattern.setStudentId(student.getId());
        pattern.setDescription(description);
        pattern.setEvidenceEntryIds(evidenceIds);
        pattern.setConfirmed(null); // waiting for the student to review it
        pattern.setCreatedAt(Instant.now());
        return pattern;
    }

    /** Builds one small, specific, optional suggestion once a pattern is confirmed. */
    public String buildSuggestion(Pattern pattern) {
        // Simple template-based suggestion, tied directly to this pattern type.
        // No AI needed here - this is the same honest, rule-based approach
        // used everywhere else in the app.
        return "Want to pick one of these back up before starting something new - " +
                "or is it fine to leave them as they are for now?";
    }

    private boolean alreadyHasSimilarUnconfirmedPattern(String studentId, Pattern candidate) {
        return patternRepository.findByStudentIdAndConfirmedIsNull(studentId).stream()
                .anyMatch(existing -> sameEvidence(existing, candidate));
    }

    private boolean sameEvidence(Pattern a, Pattern b) {
        return a.getEvidenceEntryIds() != null
                && b.getEvidenceEntryIds() != null
                && a.getEvidenceEntryIds().size() == b.getEvidenceEntryIds().size()
                && a.getEvidenceEntryIds().containsAll(b.getEvidenceEntryIds());
    }

    private long daysSince(Instant instant) {
        return ChronoUnit.DAYS.between(instant, Instant.now());
    }
}
