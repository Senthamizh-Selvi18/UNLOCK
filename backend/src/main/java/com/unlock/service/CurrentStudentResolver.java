package com.unlock.service;

import com.unlock.model.StudentUser;
import com.unlock.repository.StudentUserRepository;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Optional;

@Service
public class CurrentStudentResolver {

    private final StudentUserRepository studentUserRepository;

    public CurrentStudentResolver(StudentUserRepository studentUserRepository) {
        this.studentUserRepository = studentUserRepository;
    }

    public Optional<StudentUser> resolve(Principal principal) {
        if (principal == null) {
            return Optional.empty();
        }
        OAuth2User oAuth2User = (OAuth2User) ((AbstractAuthenticationToken) principal).getPrincipal();
        String githubId = String.valueOf(oAuth2User.getAttributes().get("id"));
        return studentUserRepository.findByGithubId(githubId);
    }
}
