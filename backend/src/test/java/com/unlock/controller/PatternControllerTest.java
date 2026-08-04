package com.unlock.controller;

import com.unlock.dto.ConfirmPatternRequest;
import com.unlock.model.Pattern;
import com.unlock.model.StudentUser;
import com.unlock.repository.PatternRepository;
import com.unlock.service.CurrentStudentResolver;
import com.unlock.service.PatternEngineService;
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
 * Tests for PatternController - the scan/list/confirm/dismiss endpoints
 * that make up the Pattern Engine + Suggestion Engine's HTTP surface.
 */
class PatternControllerTest {

    @Mock private PatternEngineService patternEngineService;
    @Mock private PatternRepository patternRepository;
    @Mock private CurrentStudentResolver currentStudentResolver;
    @Mock private Principal principal;

    private PatternController patternController;
    private StudentUser student;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        patternController = new PatternController(patternEngineService, patternRepository, currentStudentResolver);

        student = new StudentUser();
        student.setId("student-1");
    }

    @Test
    void listPatternsReturns401WhenNobodyIsLoggedIn() {
        when(currentStudentResolver.resolve(principal)).thenReturn(Optional.empty());

        var response = patternController.listPatterns(principal);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void scanReturns401WhenNobodyIsLoggedIn() {
        when(currentStudentResolver.resolve(principal)).thenReturn(Optional.empty());

        var response = patternController.scan(principal);

        assertEquals(401, response.getStatusCode().value());
        verify(patternEngineService, never()).scanForPatterns(any());
    }

    @Test
    void scanDelegatesToThePatternEngineForTheLoggedInStudent() {
        Pattern found = new Pattern();
        found.setDescription("2 of your projects haven't had activity in 30+ days");

        when(currentStudentResolver.resolve(principal)).thenReturn(Optional.of(student));
        when(patternEngineService.scanForPatterns(student)).thenReturn(List.of(found));

        var response = patternController.scan(principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void confirmReturns404WhenThePatternDoesNotExist() {
        when(patternRepository.findById("missing-id")).thenReturn(Optional.empty());
        ConfirmPatternRequest request = new ConfirmPatternRequest();
        request.setConfirmed(true);

        var response = patternController.confirm("missing-id", request);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void confirmingAsAccurateGeneratesASuggestion() {
        Pattern pattern = new Pattern();
        pattern.setId("pattern-1");

        when(patternRepository.findById("pattern-1")).thenReturn(Optional.of(pattern));
        when(patternEngineService.buildSuggestion(pattern)).thenReturn("Want to revisit one of these?");
        when(patternRepository.save(any(Pattern.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfirmPatternRequest request = new ConfirmPatternRequest();
        request.setConfirmed(true);

        var response = patternController.confirm("pattern-1", request);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().getConfirmed());
        assertEquals("Want to revisit one of these?", response.getBody().getSuggestion());
    }

    @Test
    void rejectingAPatternNeverGeneratesASuggestion() {
        Pattern pattern = new Pattern();
        pattern.setId("pattern-1");

        when(patternRepository.findById("pattern-1")).thenReturn(Optional.of(pattern));
        when(patternRepository.save(any(Pattern.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfirmPatternRequest request = new ConfirmPatternRequest();
        request.setConfirmed(false);

        var response = patternController.confirm("pattern-1", request);

        assertFalse(response.getBody().getConfirmed());
        assertNull(response.getBody().getSuggestion(), "A rejected pattern must never get a suggestion attached");
        verify(patternEngineService, never()).buildSuggestion(any());
    }

    @Test
    void dismissSuggestionMarksItDismissedSoItNeverShowsAgain() {
        Pattern pattern = new Pattern();
        pattern.setId("pattern-1");
        pattern.setSuggestionDismissed(false);

        when(patternRepository.findById("pattern-1")).thenReturn(Optional.of(pattern));
        when(patternRepository.save(any(Pattern.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = patternController.dismissSuggestion("pattern-1");

        assertTrue(response.getBody().isSuggestionDismissed());
    }

    @Test
    void dismissSuggestionReturns404ForAnUnknownPattern() {
        when(patternRepository.findById("missing-id")).thenReturn(Optional.empty());

        var response = patternController.dismissSuggestion("missing-id");

        assertEquals(404, response.getStatusCode().value());
    }
}
