package com.unlock.service;

import com.unlock.model.Reflection;
import com.unlock.model.StudentUser;
import com.unlock.repository.ReflectionRepository;
import com.unlock.repository.StudentUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the Reflection Mirror's core design promises:
 *
 *   1. The first-ever question is a gentle opener, not a heavy one.
 *   2. Every question after that is grounded in the student's own last
 *      answer - never a generic, disconnected journaling prompt.
 *   3. A student is only "due" for a new reflection if their last one
 *      is answered AND at least 14 days old - never before either
 *      condition is met.
 */
class ReflectionServiceTest {

    @Mock private ReflectionRepository reflectionRepository;
    @Mock private StudentUserRepository studentUserRepository;

    private ReflectionService reflectionService;
    private StudentUser student;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reflectionService = new ReflectionService(reflectionRepository, studentUserRepository);

        student = new StudentUser();
        student.setId("student-1");
        student.setUsername("testuser");
    }

    @Test
    void firstEverReflectionIsAGentleOpenerNotAFollowUp() {
        when(reflectionRepository.findFirstByStudentIdAndAnswerNotNullOrderByAnsweredAtDesc(student.getId()))
                .thenReturn(Optional.empty());
        when(reflectionRepository.save(any(Reflection.class))).thenAnswer(inv -> inv.getArgument(0));

        Reflection result = reflectionService.generateNow(student);

        assertFalse(result.getQuestion().contains("Last time"),
                "The very first reflection must not reference a previous answer that doesn't exist");
        assertTrue(result.getQuestion().contains("proud"),
                "First reflection should be the gentle opener question");
    }

    @Test
    void followUpQuestionReferencesTheStudentsOwnPreviousAnswer() {
        Reflection previous = new Reflection();
        previous.setAnswer("I finally finished my portfolio site");
        previous.setAnsweredAt(Instant.now().minus(20, ChronoUnit.DAYS));

        when(reflectionRepository.findFirstByStudentIdAndAnswerNotNullOrderByAnsweredAtDesc(student.getId()))
                .thenReturn(Optional.of(previous));
        when(reflectionRepository.save(any(Reflection.class))).thenAnswer(inv -> inv.getArgument(0));

        Reflection result = reflectionService.generateNow(student);

        assertTrue(result.getQuestion().contains("I finally finished my portfolio site"),
                "Follow-up question must quote the student's own previous answer, not a generic prompt");
    }

    @Test
    void studentWithNoReflectionsAtAllIsDue() {
        when(studentUserRepository.findAll()).thenReturn(List.of(student));
        when(reflectionRepository.findFirstByStudentIdOrderByCreatedAtDesc(student.getId()))
                .thenReturn(Optional.empty());
        when(reflectionRepository.findFirstByStudentIdAndAnswerNotNullOrderByAnsweredAtDesc(student.getId()))
                .thenReturn(Optional.empty());
        when(reflectionRepository.save(any(Reflection.class))).thenAnswer(inv -> inv.getArgument(0));

        reflectionService.generateForAllStudentsIfDue();

        verify(reflectionRepository, times(1)).save(any(Reflection.class));
    }

    @Test
    void studentWithAnUnansweredReflectionIsNotDueForAnotherOne() {
        Reflection unanswered = new Reflection();
        unanswered.setCreatedAt(Instant.now().minus(30, ChronoUnit.DAYS)); // old, but never answered

        when(studentUserRepository.findAll()).thenReturn(List.of(student));
        when(reflectionRepository.findFirstByStudentIdOrderByCreatedAtDesc(student.getId()))
                .thenReturn(Optional.of(unanswered));

        reflectionService.generateForAllStudentsIfDue();

        verify(reflectionRepository, never()).save(any(Reflection.class));
    }

    @Test
    void studentWhoAnsweredRecentlyIsNotDueYet() {
        Reflection answeredRecently = new Reflection();
        answeredRecently.setAnswer("Still working on it");
        answeredRecently.setCreatedAt(Instant.now().minus(5, ChronoUnit.DAYS)); // only 5 days ago

        when(studentUserRepository.findAll()).thenReturn(List.of(student));
        when(reflectionRepository.findFirstByStudentIdOrderByCreatedAtDesc(student.getId()))
                .thenReturn(Optional.of(answeredRecently));

        reflectionService.generateForAllStudentsIfDue();

        verify(reflectionRepository, never()).save(any(Reflection.class));
    }

    @Test
    void studentWhoAnsweredFourteenOrMoreDaysAgoIsDue() {
        Reflection answeredLongAgo = new Reflection();
        answeredLongAgo.setAnswer("Finished that project finally");
        answeredLongAgo.setCreatedAt(Instant.now().minus(14, ChronoUnit.DAYS)); // exactly at the threshold

        when(studentUserRepository.findAll()).thenReturn(List.of(student));
        when(reflectionRepository.findFirstByStudentIdOrderByCreatedAtDesc(student.getId()))
                .thenReturn(Optional.of(answeredLongAgo));
        when(reflectionRepository.findFirstByStudentIdAndAnswerNotNullOrderByAnsweredAtDesc(student.getId()))
                .thenReturn(Optional.of(answeredLongAgo));
        when(reflectionRepository.save(any(Reflection.class))).thenAnswer(inv -> inv.getArgument(0));

        reflectionService.generateForAllStudentsIfDue();

        verify(reflectionRepository, times(1)).save(any(Reflection.class));
    }
}
