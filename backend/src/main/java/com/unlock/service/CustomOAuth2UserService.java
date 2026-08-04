package com.unlock.service;

import com.unlock.model.StudentUser;
import com.unlock.repository.StudentUserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * This class runs automatically every time someone finishes logging in
 * with GitHub. Spring Security calls loadUser() for us - we just hook
 * into it to save the student into MongoDB.
 *
 * Flow:
 *   1. Student clicks "Login with GitHub"
 *   2. GitHub asks them to approve, then redirects back to us with a code
 *   3. Spring Security exchanges that code for the student's GitHub profile
 *   4. THIS class receives that profile and saves/updates it in Mongo
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final StudentUserRepository studentUserRepository;

    public CustomOAuth2UserService(StudentUserRepository studentUserRepository) {
        this.studentUserRepository = studentUserRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Let Spring do the actual GitHub API call to fetch the profile
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // GitHub's user profile fields: https://docs.github.com/en/rest/users/users
        String githubId = String.valueOf(oAuth2User.getAttributes().get("id"));
        String username = (String) oAuth2User.getAttributes().get("login");
        String displayName = (String) oAuth2User.getAttributes().get("name");
        String avatarUrl = (String) oAuth2User.getAttributes().get("avatar_url");
        String email = (String) oAuth2User.getAttributes().get("email"); // often null - that's fine for now

        StudentUser student = studentUserRepository.findByGithubId(githubId)
                .orElseGet(StudentUser::new);

        boolean isNewStudent = (student.getId() == null);

        student.setGithubId(githubId);
        student.setUsername(username);
        student.setDisplayName(displayName);
        student.setAvatarUrl(avatarUrl);
        student.setEmail(email);
        student.setLastLoginAt(Instant.now());
        if (isNewStudent) {
            student.setFirstLoginAt(Instant.now());
        }

        studentUserRepository.save(student);

        // Return the original OAuth2User - Spring Security uses this to
        // know who is "logged in" for the rest of the request lifecycle.
        return oAuth2User;
    }
}
