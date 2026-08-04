package com.unlock.controller;

import com.unlock.dto.GrowthReplayResponse;
import com.unlock.model.Entry;
import com.unlock.model.Pattern;
import com.unlock.model.Reflection;
import com.unlock.model.StudentUser;
import com.unlock.repository.EntryRepository;
import com.unlock.repository.PatternRepository;
import com.unlock.repository.ReflectionRepository;
import com.unlock.service.CurrentStudentResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * Growth Replay: an honest, factual summary the student can generate
 * ANYTIME they choose - never automatic, never shared by default.
 * This endpoint only reads and repackages data that already exists
 * elsewhere in the app; it never generates new claims.
 */
@RestController
@RequestMapping("/api/replay")
public class GrowthReplayController {

    private final EntryRepository entryRepository;
    private final ReflectionRepository reflectionRepository;
    private final PatternRepository patternRepository;
    private final CurrentStudentResolver currentStudentResolver;

    public GrowthReplayController(EntryRepository entryRepository,
                                   ReflectionRepository reflectionRepository,
                                   PatternRepository patternRepository,
                                   CurrentStudentResolver currentStudentResolver) {
        this.entryRepository = entryRepository;
        this.reflectionRepository = reflectionRepository;
        this.patternRepository = patternRepository;
        this.currentStudentResolver = currentStudentResolver;
    }

    @GetMapping
    public ResponseEntity<GrowthReplayResponse> getReplay(Principal principal) {
        return currentStudentResolver.resolve(principal)
                .map(this::buildReplay)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(401).build());
    }

    private GrowthReplayResponse buildReplay(StudentUser student) {
        List<Entry> entries = entryRepository.findByStudentIdOrderByDateDesc(student.getId());

        List<Reflection> answeredReflections = reflectionRepository
                .findByStudentIdOrderByCreatedAtDesc(student.getId()).stream()
                .filter(Reflection::isAnswered)
                .toList();

        // Only confirmed patterns go into the replay - never an
        // unreviewed or rejected one. This is a factual summary,
        // not a place to surface unverified guesses.
        List<Pattern> confirmedPatterns = patternRepository
                .findByStudentIdOrderByCreatedAtDesc(student.getId()).stream()
                .filter(p -> Boolean.TRUE.equals(p.getConfirmed()))
                .toList();

        return new GrowthReplayResponse(student, entries, answeredReflections, confirmedPatterns);
    }
}
