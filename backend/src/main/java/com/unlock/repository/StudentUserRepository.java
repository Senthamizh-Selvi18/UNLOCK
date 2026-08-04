package com.unlock.repository;

import com.unlock.model.StudentUser;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Spring Data auto-generates the implementation of this interface at
 * runtime. We don't write any SQL/query code - method names like
 * findByGithubId are enough for Spring to understand what we want.
 */
public interface StudentUserRepository extends MongoRepository<StudentUser, String> {
    Optional<StudentUser> findByGithubId(String githubId);
}
