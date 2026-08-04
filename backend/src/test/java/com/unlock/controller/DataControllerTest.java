package com.unlock.controller;

import com.unlock.model.Entry;
import com.unlock.model.Pattern;
import com.unlock.model.Reflection;
import com.unlock.model.StudentUser;
import com.unlock.repository.EntryRepository;
import com.unlock.repository.PatternRepository;
import com.unlock.repository.ReflectionRepository;
import com.unlock.repository.StudentUserRepository;
import com.unlock.service.CurrentStudentResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
 * Tests for DataController - the export and delete-all endpoints.
 * Delete-all gets the most scrutiny here on purpose: it's destructive,
 * unconditional, and permanent, so it's exactly the kind of code that
 * deserves a test rather than just careful-looking comments.
 */
class DataControllerTest {

    @Mock private StudentUserRepository studentUserRepository;
    @Mock private EntryRepository entryRepository;
    @Mock private ReflectionRepository reflectionRepository;
    @Mock private PatternRepository patternRepository;
    @Mock private CurrentStudentResolver currentStudentResolver;
    @Mock private Principal principal;
    @Mock private HttpServletRequest httpServletRequest;
    @Mock private HttpSession httpSession;

    private DataController dataController;
    private StudentUser student;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dataController = new DataController(
                studentUserRepository, entryRepository, reflectionRepository, patternRepository, currentStudentResolver);

        student = new StudentUser();
        student.setId("student-1");
        student.setUsername("testuser");
    }

    @Test
    void exportReturns401WhenNobodyIsLoggedIn() {
        when(currentStudentResolver.resolve(principal)).thenReturn(Optional.empty());

        var response = dataController.exportMyData(principal);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void exportIncludesEverythingNotJustConfirmedPatterns() {
        // Unlike Growth Replay, the raw export must include EVERYTHING -
        // unreviewed and rejected patterns too - since it's a backup of
        // the student's own data, not a curated presentation of it.
        Pattern unconfirmedPattern = new Pattern();
        unconfirmedPattern.setConfirmed(null);
        Pattern rejectedPattern = new Pattern();
        rejectedPattern.setConfirmed(false);

        when(currentStudentResolver.resolve(principal)).thenReturn(Optional.of(student));
        when(entryRepository.findByStudentIdOrderByDateDesc(student.getId())).thenReturn(List.of(new Entry()));
        when(reflectionRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())).thenReturn(List.of(new Reflection()));
        when(patternRepository.findByStudentIdOrderByCreatedAtDesc(student.getId()))
                .thenReturn(List.of(unconfirmedPattern, rejectedPattern));

        var response = dataController.exportMyData(principal);

        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        List<Pattern> exportedPatterns = (List<Pattern>) response.getBody().get("patterns");
        assertEquals(2, exportedPatterns.size(), "Export must include unconfirmed and rejected patterns too, not just confirmed ones");
    }

    @Test
    void deleteAllReturns401WhenNobodyIsLoggedIn() {
        when(currentStudentResolver.resolve(principal)).thenReturn(Optional.empty());

        var response = dataController.deleteAllMyData(principal, httpServletRequest);

        assertEquals(401, response.getStatusCode().value());
        verify(studentUserRepository, never()).delete(any());
    }

    @Test
    void deleteAllRemovesEveryCollectionForThatStudentAndEndsTheSession() {
        Entry entry = new Entry();
        Reflection reflection = new Reflection();
        Pattern pattern = new Pattern();

        when(currentStudentResolver.resolve(principal)).thenReturn(Optional.of(student));
        when(entryRepository.findByStudentIdOrderByDateDesc(student.getId())).thenReturn(List.of(entry));
        when(reflectionRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())).thenReturn(List.of(reflection));
        when(patternRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())).thenReturn(List.of(pattern));
        when(httpServletRequest.getSession()).thenReturn(httpSession);

        var response = dataController.deleteAllMyData(principal, httpServletRequest);

        assertEquals(200, response.getStatusCode().value());

        // Every collection must actually be cleared - not just the profile.
        verify(entryRepository).deleteAll(List.of(entry));
        verify(reflectionRepository).deleteAll(List.of(reflection));
        verify(patternRepository).deleteAll(List.of(pattern));
        verify(studentUserRepository).delete(student);

        // The session must be properly ended too - deleting the database
        // record alone would leave the browser still "logged in."
        verify(httpSession).invalidate();
    }

    @Test
    void deleteAllDoesNotTouchOtherStudentsData() {
        // A student's delete request should only ever query by their OWN
        // studentId - this test exists to make that explicit and catch
        // any future refactor that accidentally broadens the query.
        when(currentStudentResolver.resolve(principal)).thenReturn(Optional.of(student));
        when(entryRepository.findByStudentIdOrderByDateDesc(anyString())).thenReturn(List.of());
        when(reflectionRepository.findByStudentIdOrderByCreatedAtDesc(anyString())).thenReturn(List.of());
        when(patternRepository.findByStudentIdOrderByCreatedAtDesc(anyString())).thenReturn(List.of());
        when(httpServletRequest.getSession()).thenReturn(httpSession);

        dataController.deleteAllMyData(principal, httpServletRequest);

        verify(entryRepository).findByStudentIdOrderByDateDesc(eq("student-1"));
        verify(reflectionRepository).findByStudentIdOrderByCreatedAtDesc(eq("student-1"));
        verify(patternRepository).findByStudentIdOrderByCreatedAtDesc(eq("student-1"));
    }
}
