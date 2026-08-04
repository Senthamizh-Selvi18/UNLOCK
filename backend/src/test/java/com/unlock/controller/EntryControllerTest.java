package com.unlock.controller;

import com.unlock.dto.ManualEntryRequest;
import com.unlock.model.Entry;
import com.unlock.model.StudentUser;
import com.unlock.repository.EntryRepository;
import com.unlock.service.CurrentStudentResolver;
import com.unlock.service.EntrySyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for EntryController - plain unit tests against the controller
 * class directly (not a full Spring web-security context, which would
 * need real GitHub OAuth credentials just to boot). This still
 * genuinely exercises the controller logic, including the auth
 * boundary every endpoint depends on.
 */
class EntryControllerTest {

    @Mock private EntryRepository entryRepository;
    @Mock private EntrySyncService entrySyncService;
    @Mock private CurrentStudentResolver currentStudentResolver;
    @Mock private Principal principal;

    private EntryController entryController;
    private StudentUser student;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        entryController = new EntryController(entryRepository, entrySyncService, currentStudentResolver);

        student = new StudentUser();
        student.setId("student-1");
    }

    @Test
    void listEntriesReturns401WhenNobodyIsLoggedIn() {
        when(currentStudentResolver.resolve(principal)).thenReturn(Optional.empty());

        var response = entryController.listEntries(principal);

        assertEquals(401, response.getStatusCode().value());
        verify(entryRepository, never()).findByStudentIdOrderByDateDesc(anyString());
    }

    @Test
    void listEntriesReturnsTheStudentsTimelineWhenLoggedIn() {
        Entry entry = new Entry();
        entry.setTitle("my-project");

        when(currentStudentResolver.resolve(principal)).thenReturn(Optional.of(student));
        when(entryRepository.findByStudentIdOrderByDateDesc(student.getId())).thenReturn(List.of(entry));

        var response = entryController.listEntries(principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("my-project", response.getBody().get(0).getTitle());
    }

    @Test
    void addManualEntryReturns401WhenNobodyIsLoggedIn() {
        when(currentStudentResolver.resolve(principal)).thenReturn(Optional.empty());
        ManualEntryRequest request = new ManualEntryRequest();
        request.setTitle("Led a coding club session");

        var response = entryController.addManualEntry(principal, request);

        assertEquals(401, response.getStatusCode().value());
        verify(entryRepository, never()).save(any());
    }

    @Test
    void addManualEntryCreatesAnEntryMarkedAsManualSource() {
        when(currentStudentResolver.resolve(principal)).thenReturn(Optional.of(student));
        when(entryRepository.save(any(Entry.class))).thenAnswer(inv -> inv.getArgument(0));

        ManualEntryRequest request = new ManualEntryRequest();
        request.setTitle("Led a coding club session");
        request.setDescription("Taught juniors about Git basics");

        var response = entryController.addManualEntry(principal, request);

        assertEquals(200, response.getStatusCode().value());
        Entry saved = response.getBody();
        assertEquals(Entry.EntrySource.MANUAL, saved.getSource(),
                "Manual entries must always be tagged as MANUAL, never mistaken for GitHub evidence");
        assertEquals(student.getId(), saved.getStudentId());
        assertEquals("Led a coding club session", saved.getTitle());
    }
}
