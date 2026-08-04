package com.unlock.controller;

import com.unlock.dto.AnswerRequest;
import com.unlock.model.Reflection;
import com.unlock.model.StudentUser;
import com.unlock.service.CurrentStudentResolver;
import com.unlock.service.ReflectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReflectionControllerTest {

    @Mock private ReflectionService reflectionService;
    @Mock private CurrentStudentResolver currentStudentResolver;
    @Mock private Principal principal;

    private ReflectionController reflectionController;
    private StudentUser student;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reflectionController = new ReflectionController(reflectionService, currentStudentResolver);

        student = new StudentUser();
        student.setId("student-1");
    }

    @Test
    void getCurrentReturns401WhenNobodyIsLoggedIn() {
        when(currentStudentResolver.resolve(principal)).thenReturn(Optional.empty());

        var response = reflectionController.getCurrent(principal);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void getCurrentReturns204WhenNothingIsDueRightNow() {
        when(currentStudentResolver.resolve(principal)).thenReturn(Optional.of(student));
        when(reflectionService.getCurrentReflection(student.getId())).thenReturn(Optional.empty());

        var response = reflectionController.getCurrent(principal);

        assertEquals(204, response.getStatusCode().value(),
                "No pending reflection should be a quiet 204, not an error");
    }

    @Test
    void getCurrentReturnsTheQuestionWhenOneIsPending() {
        Reflection pending = new Reflection();
        pending.setQuestion("What's one thing you're proud of?");

        when(currentStudentResolver.resolve(principal)).thenReturn(Optional.of(student));
        when(reflectionService.getCurrentReflection(student.getId())).thenReturn(Optional.of(pending));

        var response = reflectionController.getCurrent(principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("What's one thing you're proud of?", response.getBody().getQuestion());
    }

    @Test
    void getHistoryReturns401WhenNobodyIsLoggedIn() {
        when(currentStudentResolver.resolve(principal)).thenReturn(Optional.empty());

        var response = reflectionController.getHistory(principal);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void getHistoryReturnsTheStudentsPastReflections() {
        Reflection past = new Reflection();
        past.setAnswer("Finished my first project");

        when(currentStudentResolver.resolve(principal)).thenReturn(Optional.of(student));
        when(reflectionService.getHistory(student.getId())).thenReturn(List.of(past));

        var response = reflectionController.getHistory(principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void answerSavesTheStudentsResponseThroughTheService() {
        Reflection answered = new Reflection();
        answered.setAnswer("Learned Docker this week");

        when(reflectionService.answer("reflection-1", "Learned Docker this week")).thenReturn(answered);

        AnswerRequest request = new AnswerRequest();
        request.setAnswer("Learned Docker this week");

        var response = reflectionController.answer("reflection-1", request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Learned Docker this week", response.getBody().getAnswer());
    }

    @Test
    void generateNowReturns401WhenNobodyIsLoggedIn() {
        when(currentStudentResolver.resolve(principal)).thenReturn(Optional.empty());

        var response = reflectionController.generateNow(principal);

        assertEquals(401, response.getStatusCode().value());
        verify(reflectionService, never()).generateNow(any());
    }

    @Test
    void generateNowCreatesAReflectionForTheLoggedInStudent() {
        Reflection generated = new Reflection();
        generated.setQuestion("What's one thing you're proud of?");

        when(currentStudentResolver.resolve(principal)).thenReturn(Optional.of(student));
        when(reflectionService.generateNow(student)).thenReturn(generated);

        var response = reflectionController.generateNow(principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("What's one thing you're proud of?", response.getBody().getQuestion());
    }
}
