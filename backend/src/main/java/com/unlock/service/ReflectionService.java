package com.unlock.service;

import com.unlock.model.Reflection;
import com.unlock.model.StudentUser;
import com.unlock.repository.ReflectionRepository;
import com.unlock.repository.StudentUserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * The Reflection Mirror: every two weeks, one grounded question, built
 * from what the student said last time - never a generic journaling
 * prompt, and never a verdict. Follows the friendly-wording rules:
 * always a question, never a label.
 */
@Service
public class ReflectionService {

    private static final int DAYS_BETWEEN_REFLECTIONS = 14;

    private final ReflectionRepository reflectionRepository;
    private final StudentUserRepository studentUserRepository;

    public ReflectionService(ReflectionRepository reflectionRepository,
                              StudentUserRepository studentUserRepository) {
        this.reflectionRepository = reflectionRepository;
        this.studentUserRepository = studentUserRepository;
    }

    /**
     * Returns the student's current unanswered reflection, if one exists.
     * Does NOT create one - use generateIfDue() or generateNow() for that.
     * Keeping "read" and "create" separate avoids accidentally creating
     * a new reflection just because the student opened the page.
     */
    public Optional<Reflection> getCurrentReflection(String studentId) {
        return reflectionRepository.findFirstByStudentIdOrderByCreatedAtDesc(studentId)
                .filter(r -> !r.isAnswered());
    }

    public List<Reflection> getHistory(String studentId) {
        return reflectionRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
    }

    public Reflection answer(String reflectionId, String answerText) {
        Reflection reflection = reflectionRepository.findById(reflectionId)
                .orElseThrow(() -> new IllegalArgumentException("Reflection not found"));
        reflection.setAnswer(answerText);
        reflection.setAnsweredAt(Instant.now());
        return reflectionRepository.save(reflection);
    }

    /**
     * Creates a new reflection right now, regardless of timing.
     * Used for manual "generate now" during development/demo, and is
     * also the method the scheduled job calls when a student is due.
     */
    public Reflection generateNow(StudentUser student) {
        String question = buildQuestion(student);

        Reflection reflection = new Reflection();
        reflection.setStudentId(student.getId());
        reflection.setQuestion(question);
        reflection.setCreatedAt(Instant.now());
        return reflectionRepository.save(reflection);
    }

    /**
     * Runs automatically once a day and creates a new reflection for any
     * student whose last one is 14+ days old (or who has never had one).
     * Cron format: second minute hour day-of-month month day-of-week
     * "0 0 8 * * *" = every day at 8:00 AM server time.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void generateForAllStudentsIfDue() {
        for (StudentUser student : studentUserRepository.findAll()) {
            Optional<Reflection> latest =
                    reflectionRepository.findFirstByStudentIdOrderByCreatedAtDesc(student.getId());

            boolean due = latest.isEmpty()
                    || (latest.get().isAnswered()
                        && daysSince(latest.get().getCreatedAt()) >= DAYS_BETWEEN_REFLECTIONS);

            if (due) {
                generateNow(student);
            }
        }
    }

    private long daysSince(Instant instant) {
        return ChronoUnit.DAYS.between(instant, Instant.now());
    }

    /**
     * Builds the actual question text. Follows the friendly-wording rules:
     * grounded in the student's own past words where possible, always
     * phrased as a genuine question, never a statement about who they are.
     */
    private String buildQuestion(StudentUser student) {
        Optional<Reflection> lastAnswered =
                reflectionRepository.findFirstByStudentIdAndAnswerNotNullOrderByAnsweredAtDesc(student.getId());

        if (lastAnswered.isPresent()) {
            String previousAnswer = lastAnswered.get().getAnswer();
            return "Last time, you said: \"" + previousAnswer + "\" — is that still true, " +
                    "or has something changed since then?";
        }

        // First-ever reflection for this student - a gentle opener, not
        // a heavy question, since there's no history to build on yet.
        return "What's one thing you've been working on lately that you're a little proud of?";
    }
}
