package com.unlock.controller;

import com.unlock.dto.ConfirmPatternRequest;
import com.unlock.model.Pattern;
import com.unlock.model.StudentUser;
import com.unlock.repository.PatternRepository;
import com.unlock.service.CurrentStudentResolver;
import com.unlock.service.PatternEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/patterns")
public class PatternController {

    private final PatternEngineService patternEngineService;
    private final PatternRepository patternRepository;
    private final CurrentStudentResolver currentStudentResolver;

    public PatternController(PatternEngineService patternEngineService,
                              PatternRepository patternRepository,
                              CurrentStudentResolver currentStudentResolver) {
        this.patternEngineService = patternEngineService;
        this.patternRepository = patternRepository;
        this.currentStudentResolver = currentStudentResolver;
    }

    /** All patterns for this student, most recent first. */
    @GetMapping
    public ResponseEntity<List<Pattern>> listPatterns(Principal principal) {
        return currentStudentResolver.resolve(principal)
                .map(student -> ResponseEntity.ok(
                        patternRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())))
                .orElse(ResponseEntity.status(401).build());
    }

    /**
     * Scans the student's evidence for new patterns right now.
     * This is a manual trigger for instant feedback in the UI - patterns
     * also get scanned automatically once a day for every student via
     * the scheduled job in PatternEngineService.
     */
    @PostMapping("/scan")
    public ResponseEntity<List<Pattern>> scan(Principal principal) {
        return currentStudentResolver.resolve(principal)
                .map(student -> ResponseEntity.ok(patternEngineService.scanForPatterns(student)))
                .orElse(ResponseEntity.status(401).build());
    }

    /**
     * The student confirms or rejects a pattern.
     * If confirmed, we generate one optional suggestion tied to it.
     * If rejected, the pattern stays in history but is marked not accurate
     * and never contributes a suggestion.
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Pattern> confirm(@PathVariable String id, @RequestBody ConfirmPatternRequest request) {
        Optional<Pattern> maybePattern = patternRepository.findById(id);
        if (maybePattern.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Pattern pattern = maybePattern.get();
        pattern.setConfirmed(request.isConfirmed());

        if (request.isConfirmed()) {
            pattern.setSuggestion(patternEngineService.buildSuggestion(pattern));
        }

        return ResponseEntity.ok(patternRepository.save(pattern));
    }

    /**
     * Student dismisses a suggestion - per the friendly-wording rules,
     * we never show this exact suggestion again after this.
     */
    @PostMapping("/{id}/dismiss-suggestion")
    public ResponseEntity<Pattern> dismissSuggestion(@PathVariable String id) {
        Optional<Pattern> maybePattern = patternRepository.findById(id);
        if (maybePattern.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Pattern pattern = maybePattern.get();
        pattern.setSuggestionDismissed(true);
        return ResponseEntity.ok(patternRepository.save(pattern));
    }
}
