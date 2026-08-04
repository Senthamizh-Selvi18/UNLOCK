package com.unlock.service;

import com.unlock.model.Entry;
import com.unlock.model.Pattern;
import com.unlock.model.StudentUser;
import com.unlock.repository.EntryRepository;
import com.unlock.repository.PatternRepository;
import com.unlock.repository.StudentUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the Pattern Engine's core honesty rules. These aren't just
 * "does the code run" tests - they exist specifically to lock in the
 * design promises made throughout this project:
 *
 *   1. A single stalled project must NEVER become a "pattern."
 *   2. Multiple stalled projects, with real evidence attached, SHOULD.
 *   3. Scanning twice must never create a duplicate pattern.
 *
 * If a future change breaks any of these, these tests should fail loudly.
 */
class PatternEngineServiceTest {

    @Mock private EntryRepository entryRepository;
    @Mock private PatternRepository patternRepository;
    @Mock private StudentUserRepository studentUserRepository;

    private PatternEngineService patternEngineService;
    private StudentUser student;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        patternEngineService = new PatternEngineService(entryRepository, patternRepository, studentUserRepository);

        student = new StudentUser();
        student.setId("student-1");
        student.setUsername("testuser");
    }

    @Test
    void doesNotCreateAPatternFromASingleStalledProject() {
        // Only one stalled repo - should stay quiet, per the design rule
        // that one rough patch is never enough evidence for a "pattern."
        Entry stalledRepo = stalledGithubEntry("lonely-project", 45);
        when(entryRepository.findByStudentIdOrderByDateDesc(student.getId()))
                .thenReturn(List.of(stalledRepo));

        List<Pattern> result = patternEngineService.scanForPatterns(student);

        assertTrue(result.isEmpty(), "A single stalled project must never produce a pattern");
        verify(patternRepository, never()).save(any());
    }

    @Test
    void createsAPatternWhenTwoOrMoreProjectsAreStalled() {
        Entry stalledRepo1 = stalledGithubEntry("project-one", 40);
        Entry stalledRepo2 = stalledGithubEntry("project-two", 60);
        when(entryRepository.findByStudentIdOrderByDateDesc(student.getId()))
                .thenReturn(List.of(stalledRepo1, stalledRepo2));
        when(patternRepository.findByStudentIdAndConfirmedIsNull(student.getId()))
                .thenReturn(List.of()); // no existing patterns yet
        when(patternRepository.save(any(Pattern.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<Pattern> result = patternEngineService.scanForPatterns(student);

        assertEquals(1, result.size(), "Two stalled projects should produce exactly one pattern");
        Pattern pattern = result.get(0);

        // The pattern must carry real evidence - never just a bare claim.
        assertNotNull(pattern.getEvidenceEntryIds());
        assertEquals(2, pattern.getEvidenceEntryIds().size());
        assertTrue(pattern.getDescription().contains("project-one") || pattern.getDescription().contains("2 of your projects"));

        // Must start unreviewed - never pre-confirmed on the student's behalf.
        assertNull(pattern.getConfirmed());
    }

    @Test
    void doesNotIgnoreRecentlyActiveProjectsWhenCountingStalledOnes() {
        Entry stalledRepo = stalledGithubEntry("old-project", 90);
        Entry activeRepo = stalledGithubEntry("active-project", 2); // recent activity
        when(entryRepository.findByStudentIdOrderByDateDesc(student.getId()))
                .thenReturn(List.of(stalledRepo, activeRepo));

        List<Pattern> result = patternEngineService.scanForPatterns(student);

        // Only 1 truly stalled repo (the recent one shouldn't count) -
        // still not enough evidence for a pattern.
        assertTrue(result.isEmpty(), "An actively-updated project must not count toward a stalled pattern");
    }

    @Test
    void scanningTwiceDoesNotCreateADuplicatePattern() {
        Entry stalledRepo1 = stalledGithubEntry("project-one", 40);
        Entry stalledRepo2 = stalledGithubEntry("project-two", 60);
        List<Entry> entries = List.of(stalledRepo1, stalledRepo2);

        when(entryRepository.findByStudentIdOrderByDateDesc(student.getId())).thenReturn(entries);
        when(patternRepository.save(any(Pattern.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // First scan: no existing patterns yet.
        when(patternRepository.findByStudentIdAndConfirmedIsNull(student.getId())).thenReturn(new ArrayList<>());
        List<Pattern> firstScan = patternEngineService.scanForPatterns(student);
        assertEquals(1, firstScan.size());

        // Second scan: now simulate that the pattern from the first scan already exists.
        when(patternRepository.findByStudentIdAndConfirmedIsNull(student.getId())).thenReturn(firstScan);
        List<Pattern> secondScan = patternEngineService.scanForPatterns(student);

        assertTrue(secondScan.isEmpty(), "Scanning again with the same evidence must not create a duplicate pattern");
    }

    @Test
    void completedProjectsAreNeverCountedAsStalledEvenIfOld() {
        // The real bug this test guards against: a finished project going
        // quiet looks identical to an abandoned one, unless completion is
        // tracked explicitly. Marking something "completed" must remove it
        // from stalled-detection entirely, no matter how old it is.
        Entry completed1 = stalledGithubEntry("finished-project-one", 200);
        completed1.setCompleted(true);
        Entry completed2 = stalledGithubEntry("finished-project-two", 300);
        completed2.setCompleted(true);

        when(entryRepository.findByStudentIdOrderByDateDesc(student.getId()))
                .thenReturn(List.of(completed1, completed2));

        List<Pattern> result = patternEngineService.scanForPatterns(student);

        assertTrue(result.isEmpty(), "Completed projects must never be counted toward a stalled pattern");
    }

    @Test
    void mixOfCompletedAndAbandonedOnlyCountsTheAbandonedOnes() {
        Entry completed = stalledGithubEntry("finished-project", 200);
        completed.setCompleted(true);
        Entry abandoned1 = stalledGithubEntry("abandoned-one", 40);
        Entry abandoned2 = stalledGithubEntry("abandoned-two", 60);

        when(entryRepository.findByStudentIdOrderByDateDesc(student.getId()))
                .thenReturn(List.of(completed, abandoned1, abandoned2));
        when(patternRepository.findByStudentIdAndConfirmedIsNull(student.getId())).thenReturn(List.of());
        when(patternRepository.save(any(Pattern.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Pattern> result = patternEngineService.scanForPatterns(student);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getEvidenceEntryIds().size(),
                "Only the 2 truly abandoned projects should count - the completed one must be excluded");
    }

    private Entry stalledGithubEntry(String title, int daysSinceActivity) {
        Entry entry = new Entry();
        entry.setId("entry-" + title);
        entry.setTitle(title);
        entry.setSource(Entry.EntrySource.GITHUB_REPO);
        entry.setDate(Instant.now().minus(daysSinceActivity, ChronoUnit.DAYS));
        return entry;
    }
}
