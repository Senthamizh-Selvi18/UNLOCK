package com.unlock.controller;

import com.unlock.model.StudentUser;
import com.unlock.repository.StudentUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Small API the frontend calls to answer: "who is currently logged in?"
 * If nobody is logged in, Spring Security will already have blocked the
 * request before it even reaches here (see SecurityConfig).
 */
@RestController
public class AuthController {

    private final StudentUserRepository studentUserRepository;

    public AuthController(StudentUserRepository studentUserRepository) {
        this.studentUserRepository = studentUserRepository;
    }

    @GetMapping("/api/me")
    public ResponseEntity<StudentUser> getCurrentStudent(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        // For GitHub OAuth2 login, the principal's "name" is the GitHub numeric id
        // because that's the field Spring Security uses as the default name-attribute-key
        // for GitHub in this setup version. We look the student up by that id.
        OAuth2User oAuth2User = (OAuth2User) ((org.springframework.security.authentication.AbstractAuthenticationToken) principal).getPrincipal();
        String githubId = String.valueOf(oAuth2User.getAttributes().get("id"));

        return studentUserRepository.findByGithubId(githubId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).build());
    }
}
