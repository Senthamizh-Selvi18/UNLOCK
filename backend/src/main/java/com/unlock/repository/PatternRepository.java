package com.unlock.repository;

import com.unlock.model.Pattern;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PatternRepository extends MongoRepository<Pattern, String> {

    List<Pattern> findByStudentIdOrderByCreatedAtDesc(String studentId);

    // Used to avoid creating the exact same pattern over and over every scan
    List<Pattern> findByStudentIdAndConfirmedIsNull(String studentId);
}
