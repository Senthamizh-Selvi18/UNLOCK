package com.unlock.service;

import com.unlock.dto.GitHubRepoDto;
import com.unlock.model.Entry;
import com.unlock.model.StudentUser;
import com.unlock.repository.EntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the Evidence Vault sync logic - specifically the promise
 * made throughout the README: "syncing again won't create duplicates."
 * This is easy to silently break with a small refactor, so it's worth
 * locking in with a real test rather than just a comment.
 */
class EntrySyncServiceTest {

    @Mock private GitHubApiService gitHubApiService;
    @Mock private EntryRepository entryRepository;

    private EntrySyncService entrySyncService;
    private StudentUser student;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        entrySyncService = new EntrySyncService(gitHubApiService, entryRepository);

        student = new StudentUser();
        student.setId("student-1");
    }

    @Test
    void firstSyncCreatesNewEntriesForEveryRepo() {
        GitHubRepoDto repo1 = repoDto("project-alpha", "Alpha project");
        GitHubRepoDto repo2 = repoDto("project-beta", "Beta project");

        when(gitHubApiService.fetchRepos("token-123")).thenReturn(List.of(repo1, repo2));
        // No existing entries for either repo yet - both are brand new.
        when(entryRepository.findByStudentIdAndExternalId(eq(student.getId()), anyString()))
                .thenReturn(Optional.empty());
        when(entryRepository.save(any(Entry.class))).thenAnswer(inv -> inv.getArgument(0));

        int newCount = entrySyncService.syncGitHubRepos(student, "token-123");

        assertEquals(2, newCount, "Both repos are new, so newCount should be 2");
        verify(entryRepository, times(2)).save(any(Entry.class));
    }

    @Test
    void syncingAgainWithTheSameRepoUpdatesInsteadOfDuplicating() {
        GitHubRepoDto repo = repoDto("project-alpha", "Alpha project, now with a better description");

        Entry existingEntry = new Entry();
        existingEntry.setId("existing-entry-id"); // has an ID -> it's not new
        existingEntry.setStudentId(student.getId());
        existingEntry.setExternalId("student1/project-alpha");
        existingEntry.setTitle("project-alpha");
        existingEntry.setDescription("Old description");

        when(gitHubApiService.fetchRepos("token-123")).thenReturn(List.of(repo));
        when(entryRepository.findByStudentIdAndExternalId(student.getId(), "student1/project-alpha"))
                .thenReturn(Optional.of(existingEntry));
        when(entryRepository.save(any(Entry.class))).thenAnswer(inv -> inv.getArgument(0));

        int newCount = entrySyncService.syncGitHubRepos(student, "token-123");

        assertEquals(0, newCount, "Re-syncing an already-known repo must not count as a new entry");
        verify(entryRepository, times(1)).save(any(Entry.class));

        // Confirm it actually updated the existing entry's description,
        // rather than silently ignoring the fresh data from GitHub.
        verify(entryRepository).save(argThat(saved ->
                saved.getId().equals("existing-entry-id")
                        && saved.getDescription().contains("better description")
        ));
    }

    @Test
    void blankDescriptionFromGitHubBecomesAFriendlyPlaceholderNotAnEmptyString() {
        GitHubRepoDto repo = repoDto("no-description-repo", null);

        when(gitHubApiService.fetchRepos("token-123")).thenReturn(List.of(repo));
        when(entryRepository.findByStudentIdAndExternalId(eq(student.getId()), anyString()))
                .thenReturn(Optional.empty());
        when(entryRepository.save(any(Entry.class))).thenAnswer(inv -> inv.getArgument(0));

        entrySyncService.syncGitHubRepos(student, "token-123");

        verify(entryRepository).save(argThat(saved ->
                saved.getDescription().equals("No description added yet")
        ));
    }

    private GitHubRepoDto repoDto(String name, String description) {
        GitHubRepoDto dto = new GitHubRepoDto();
        dto.setName(name);
        dto.setFullName("student1/" + name);
        dto.setDescription(description);
        dto.setHtmlUrl("https://github.com/student1/" + name);
        dto.setPushedAt("2026-01-15T10:00:00Z");
        dto.setCreatedAt("2025-09-01T10:00:00Z");
        dto.setFork(false);
        return dto;
    }
}
