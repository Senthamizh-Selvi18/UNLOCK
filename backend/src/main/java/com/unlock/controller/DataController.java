package com.unlock.controller;

import com.unlock.model.StudentUser;
import com.unlock.repository.EntryRepository;
import com.unlock.repository.PatternRepository;
import com.unlock.repository.ReflectionRepository;
import com.unlock.repository.StudentUserRepository;
import com.unlock.service.CurrentStudentResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gives the student real, working control over their own data -
 * not just a policy promise. Two things, both unconditional:
 *
 *   1. Download everything, in one file, anytime.
 *   2. Delete everything, permanently, anytime - no support ticket,
 *      no waiting period, no "are you sure you're sure" dark patterns.
 */
@RestController
@RequestMapping("/api/data")
public class DataController {

    private final StudentUserRepository studentUserRepository;
    private final EntryRepository entryRepository;
    private final ReflectionRepository reflectionRepository;
    private final PatternRepository patternRepository;
    private final CurrentStudentResolver currentStudentResolver;

    public DataController(StudentUserRepository studentUserRepository,
                           EntryRepository entryRepository,
                           ReflectionRepository reflectionRepository,
                           PatternRepository patternRepository,
                           CurrentStudentResolver currentStudentResolver) {
        this.studentUserRepository = studentUserRepository;
        this.entryRepository = entryRepository;
        this.reflectionRepository = reflectionRepository;
        this.patternRepository = patternRepository;
        this.currentStudentResolver = currentStudentResolver;
    }

    /**
     * Everything the app has about this student, as one downloadable
     * JSON file - not a summary, the actual raw records. This is
     * deliberately more complete than Growth Replay, which only shows
     * confirmed patterns; this export includes everything, including
     * unreviewed and rejected patterns, because it's a backup of the
     * student's own data, not a presentation of it.
     */
    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> exportMyData(Principal principal) {
        return currentStudentResolver.resolve(principal)
                .map(student -> {
                    Map<String, Object> export = new LinkedHashMap<>();
                    export.put("exportedAt", java.time.Instant.now());
                    export.put("profile", student);
                    export.put("entries", entryRepository.findByStudentIdOrderByDateDesc(student.getId()));
                    export.put("reflections", reflectionRepository.findByStudentIdOrderByCreatedAtDesc(student.getId()));
                    export.put("patterns", patternRepository.findByStudentIdOrderByCreatedAtDesc(student.getId()));

                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"unlock-my-data.json\"")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(export);
                })
                .orElse(ResponseEntity.status(401).build());
    }

    /**
     * Permanently deletes everything this student has - profile, entries,
     * reflections, and patterns - then ends their session. Unconditional
     * and immediate on purpose: real data control means no friction when
     * someone wants out.
     */
    @DeleteMapping("/delete-all")
    public ResponseEntity<Map<String, String>> deleteAllMyData(Principal principal, HttpServletRequest request) {
        StudentUser student = currentStudentResolver.resolve(principal).orElse(null);
        if (student == null) {
            return ResponseEntity.status(401).build();
        }

        entryRepository.deleteAll(entryRepository.findByStudentIdOrderByDateDesc(student.getId()));
        reflectionRepository.deleteAll(reflectionRepository.findByStudentIdOrderByCreatedAtDesc(student.getId()));
        patternRepository.deleteAll(patternRepository.findByStudentIdOrderByCreatedAtDesc(student.getId()));
        studentUserRepository.delete(student);

        // End the session properly so the browser is fully logged out,
        // not just missing a database record.
        request.getSession().invalidate();
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}
