package com.unlock.repository;

import com.unlock.model.Entry;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface EntryRepository extends MongoRepository<Entry, String> {

    List<Entry> findByStudentIdOrderByDateDesc(String studentId);

    // Used to check "have we already saved this exact GitHub repo before?"
    // so syncing again doesn't create duplicate entries.
    Optional<Entry> findByStudentIdAndExternalId(String studentId, String externalId);
}
