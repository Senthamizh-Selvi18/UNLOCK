package com.unlock.repository;

import com.unlock.model.Reflection;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ReflectionRepository extends MongoRepository<Reflection, String> {

    List<Reflection> findByStudentIdOrderByCreatedAtDesc(String studentId);

    Optional<Reflection> findFirstByStudentIdOrderByCreatedAtDesc(String studentId);

    // The most recent one that actually has an answer - used to build the
    // "last time you said..." follow-up question.
    Optional<Reflection> findFirstByStudentIdAndAnswerNotNullOrderByAnsweredAtDesc(String studentId);
}
