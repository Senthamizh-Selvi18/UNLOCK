package com.unlock.service;

import com.unlock.dto.GitHubRepoDto;
import com.unlock.model.Entry;
import com.unlock.model.StudentUser;
import com.unlock.repository.EntryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * The actual "Evidence Vault" logic: fetch real GitHub data, turn it into
 * honest Entry records, and save it - updating existing entries instead
 * of duplicating them if the student syncs again later.
 */
@Service
public class EntrySyncService {

    private final GitHubApiService gitHubApiService;
    private final EntryRepository entryRepository;

    public EntrySyncService(GitHubApiService gitHubApiService, EntryRepository entryRepository) {
        this.gitHubApiService = gitHubApiService;
        this.entryRepository = entryRepository;
    }

    /**
     * Pulls the student's current GitHub repos and saves/updates them as Entries.
     * Returns how many were newly added, so the UI can show something like
     * "found 3 new things you've been working on" instead of a robotic log.
     */
    public int syncGitHubRepos(StudentUser student, String accessToken) {
        List<GitHubRepoDto> repos = gitHubApiService.fetchRepos(accessToken);
        int newCount = 0;

        for (GitHubRepoDto repo : repos) {
            Entry entry = entryRepository
                    .findByStudentIdAndExternalId(student.getId(), repo.getFullName())
                    .orElseGet(Entry::new);

            boolean isNew = (entry.getId() == null);

            entry.setStudentId(student.getId());
            entry.setTitle(repo.getName());
            entry.setDescription(
                    (repo.getDescription() == null || repo.getDescription().isBlank())
                            ? "No description added yet"
                            : repo.getDescription()
            );
            entry.setSource(Entry.EntrySource.GITHUB_REPO);
            entry.setExternalId(repo.getFullName());
            entry.setEvidenceLink(repo.getHtmlUrl());
            entry.setDate(parseGitHubDate(repo.getPushedAt()));
            entry.setUpdatedAt(Instant.now());
            if (isNew) {
                entry.setCreatedAt(Instant.now());
                newCount++;
            }

            entryRepository.save(entry);
        }

        return newCount;
    }

    private Instant parseGitHubDate(String isoDate) {
        if (isoDate == null) {
            return Instant.now();
        }
        return Instant.from(DateTimeFormatter.ISO_INSTANT.parse(isoDate));
    }
}
