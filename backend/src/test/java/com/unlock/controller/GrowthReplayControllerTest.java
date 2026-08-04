package com.unlock.controller;

import com.unlock.model.Entry;
import com.unlock.model.Pattern;
import com.unlock.model.Reflection;
import com.unlock.model.StudentUser;
import com.unlock.repository.EntryRepository;
import com.unlock.repository.PatternRepository;
import com.unlock.repository.ReflectionRepository;
import com.unlock.service.CurrentStudentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for GrowthReplayController - most important thing to lock in
 * here is the filtering: unlike the raw data export, Growth Replay must
 * NEVER include an unanswered reflection or an unconfirmed/rejected
 * pattern, since it's meant to be something honest enough to show
 * someone else if the student chooses to.
 */
class GrowthReplayControllerTest {

    @Mock private EntryRepository entryRepository;
    @Mock private ReflectionRepository reflectionRepository;
    @Mock private PatternRepository patternRepository;
    @Mock private CurrentStudentResolver currentStudentResolver;
    @Mock private Principal principal;

    private GrowthReplayController growthReplayController;
    private StudentUser student;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        growthReplayController = new GrowthReplayController(
                entryRepository, reflectionRepository, patternRepository, currentStudentResolver);

        student = new StudentUser();
        student.setId("student-1");
    }

    @Test
    void returns401WhenNobodyIsLoggedIn() {
        when(currentStudentResolver.resolve(principal)).thenReturn(Optional.empty());

        var response = growthReplayController.getReplay(principal);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void excludesUnansweredReflectionsFromTheReplay() {
        Reflection answered = new Reflection();
        answered.setAnswer("Finished my portfolio site");
        Reflection unanswered = new Reflection(); // no answer set

        when(currentStudentResolver.resolve(principal)).thenReturn(Optional.of(student));
        when(entryRepository.findByStudentIdOrderByDateDesc(student.getId())).thenReturn(List.of());
        when(reflectionRepository.findByStudentIdOrderByCreatedAtDesc(student.getId()))
                .thenReturn(List.of(answered, unanswered));
        when(patternRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())).thenReturn(List.of());

        var response = growthReplayController.getReplay(principal);

        assertEquals(1, response.getBody().getAnsweredReflections().size(),
                "Growth Replay must only include reflections the student actually answered");
    }

    @Test
    void excludesUnconfirmedAndRejectedPatternsFromTheReplay() {
        Pattern confirmed = new Pattern();
        confirmed.setConfirmed(true);
        Pattern unconfirmed = new Pattern();
        unconfirmed.setConfirmed(null);
        Pattern rejected = new Pattern();
        rejected.setConfirmed(false);

        when(currentStudentResolver.resolve(principal)).thenReturn(Optional.of(student));
        when(entryRepository.findByStudentIdOrderByDateDesc(student.getId())).thenReturn(List.of());
        when(reflectionRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())).thenReturn(List.of());
        when(patternRepository.findByStudentIdOrderByCreatedAtDesc(student.getId()))
                .thenReturn(List.of(confirmed, unconfirmed, rejected));

        var response = growthReplayController.getReplay(principal);

        assertEquals(1, response.getBody().getConfirmedPatterns().size(),
                "Growth Replay must only ever include patterns the student personally confirmed as accurate");
    }

    @Test
    void includesAllEntriesRegardlessOfSource() {
        Entry githubEntry = new Entry();
        githubEntry.setSource(Entry.EntrySource.GITHUB_REPO);
        Entry manualEntry = new Entry();
        manualEntry.setSource(Entry.EntrySource.MANUAL);

        when(currentStudentResolver.resolve(principal)).thenReturn(Optional.of(student));
        when(entryRepository.findByStudentIdOrderByDateDesc(student.getId()))
                .thenReturn(List.of(githubEntry, manualEntry));
        when(reflectionRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())).thenReturn(List.of());
        when(patternRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())).thenReturn(List.of());

        var response = growthReplayController.getReplay(principal);

        assertEquals(2, response.getBody().getEntries().size(),
                "Unlike patterns and reflections, all entries should appear regardless of source");
    }
}
