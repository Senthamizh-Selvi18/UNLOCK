package com.unlock.controller;

import com.unlock.dto.AnswerRequest;
import com.unlock.model.Reflection;
import com.unlock.model.StudentUser;
import com.unlock.service.CurrentStudentResolver;
import com.unlock.service.ReflectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/reflections")
public class ReflectionController {

    private final ReflectionService reflectionService;
    private final CurrentStudentResolver currentStudentResolver;

    public ReflectionController(ReflectionService reflectionService,
                                 CurrentStudentResolver currentStudentResolver) {
        this.reflectionService = reflectionService;
        this.currentStudentResolver = currentStudentResolver;
    }

    /** The student's current unanswered question, if one is due. */
    @GetMapping("/current")
    public ResponseEntity<Reflection> getCurrent(Principal principal) {
        return currentStudentResolver.resolve(principal)
                .map(student -> reflectionService.getCurrentReflection(student.getId())
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.noContent().build()))
                .orElse(ResponseEntity.status(401).build());
    }

    /** Full reflection history, most recent first. */
    @GetMapping
    public ResponseEntity<List<Reflection>> getHistory(Principal principal) {
        return currentStudentResolver.resolve(principal)
                .map(student -> ResponseEntity.ok(reflectionService.getHistory(student.getId())))
                .orElse(ResponseEntity.status(401).build());
    }

    /** Submits an answer to a specific reflection. */
    @PostMapping("/{id}/answer")
    public ResponseEntity<Reflection> answer(@PathVariable String id, @RequestBody AnswerRequest request) {
        return ResponseEntity.ok(reflectionService.answer(id, request.getAnswer()));
    }

    /**
     * Manually creates a new reflection right now - mainly for development
     * and demos, since waiting 14 real days isn't practical while building.
     * In production, the scheduled job in ReflectionService does this
     * automatically every two weeks.
     */
    @PostMapping("/generate")
    public ResponseEntity<Reflection> generateNow(Principal principal) {
        return currentStudentResolver.resolve(principal)
                .map(student -> ResponseEntity.ok(reflectionService.generateNow(student)))
                .orElse(ResponseEntity.status(401).build());
    }
}
