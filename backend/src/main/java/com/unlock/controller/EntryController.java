package com.unlock.controller;

import com.unlock.dto.ManualEntryRequest;
import com.unlock.model.Entry;
import com.unlock.model.StudentUser;
import com.unlock.repository.EntryRepository;
import com.unlock.service.CurrentStudentResolver;
import com.unlock.service.EntrySyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/entries")
public class EntryController {

    private final EntryRepository entryRepository;
    private final EntrySyncService entrySyncService;
    private final CurrentStudentResolver currentStudentResolver;

    public EntryController(EntryRepository entryRepository,
                            EntrySyncService entrySyncService,
                            CurrentStudentResolver currentStudentResolver) {
        this.entryRepository = entryRepository;
        this.entrySyncService = entrySyncService;
        this.currentStudentResolver = currentStudentResolver;
    }

    /** Returns the student's full timeline, most recent first. */
    @GetMapping
    public ResponseEntity<List<Entry>> listEntries(Principal principal) {
        return currentStudentResolver.resolve(principal)
                .map(student -> ResponseEntity.ok(
                        entryRepository.findByStudentIdOrderByDateDesc(student.getId())))
                .orElse(ResponseEntity.status(401).build());
    }

    /**
     * Pulls fresh data from GitHub right now and saves it.
     * @RegisteredOAuth2AuthorizedClient gives us the student's GitHub
     * access token automatically - Spring Security stored it during login.
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncGitHub(
            Principal principal,
            @RegisteredOAuth2AuthorizedClient("github") OAuth2AuthorizedClient authorizedClient) {

        return currentStudentResolver.resolve(principal)
                .map(student -> {
                    String accessToken = authorizedClient.getAccessToken().getTokenValue();
                    int newCount = entrySyncService.syncGitHubRepos(student, accessToken);

                    String message = newCount > 0
                            ? "Found " + newCount + " new thing" + (newCount == 1 ? "" : "s") + " you've been working on."
                            : "All caught up - nothing new since last time.";

                    return ResponseEntity.ok(Map.<String, Object>of(
                            "newEntries", newCount,
                            "message", message
                    ));
                })
                .orElse(ResponseEntity.status(401).build());
    }

    /** Adds something manual - for things GitHub can never see. */
    @PostMapping("/manual")
    public ResponseEntity<Entry> addManualEntry(Principal principal, @RequestBody ManualEntryRequest request) {
        return currentStudentResolver.resolve(principal)
                .map(student -> {
                    Entry entry = new Entry();
                    entry.setStudentId(student.getId());
                    entry.setTitle(request.getTitle());
                    entry.setDescription(request.getDescription());
                    entry.setSource(Entry.EntrySource.MANUAL);
                    entry.setDate(Instant.now());
                    entry.setCreatedAt(Instant.now());
                    entry.setUpdatedAt(Instant.now());
                    return ResponseEntity.ok(entryRepository.save(entry));
                })
                .orElse(ResponseEntity.status(401).build());
    }

    /**
     * Marks (or unmarks) an entry as completed. This is the fix for a real
     * gap: without this, a finished project and an abandoned one look
     * identical to the Pattern Engine (both just go quiet). The student
     * marks completion explicitly - it's never guessed.
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<Entry> markComplete(@PathVariable String id, @RequestBody Map<String, Boolean> body) {
        return entryRepository.findById(id)
                .map(entry -> {
                    entry.setCompleted(Boolean.TRUE.equals(body.get("completed")));
                    entry.setUpdatedAt(Instant.now());
                    return ResponseEntity.ok(entryRepository.save(entry));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
