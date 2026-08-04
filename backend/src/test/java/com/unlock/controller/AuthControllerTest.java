package com.unlock.controller;

import com.unlock.model.StudentUser;
import com.unlock.repository.StudentUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for AuthController's /api/me endpoint. Uses a real
 * OAuth2AuthenticationToken (rather than a mock) wrapping a real
 * DefaultOAuth2User, since the controller specifically casts the
 * principal and reads GitHub's "id" attribute out of it - a plain
 * mock Principal wouldn't exercise that real code path honestly.
 */
class AuthControllerTest {

    @Mock private StudentUserRepository studentUserRepository;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authController = new AuthController(studentUserRepository);
    }

    @Test
    void returns401WhenPrincipalIsNull() {
        var response = authController.getCurrentStudent(null);

        assertEquals(401, response.getStatusCode().value());
        verifyNoInteractions(studentUserRepository);
    }

    @Test
    void returnsTheStudentWhenGithubIdMatchesAnExistingRecord() {
        OAuth2AuthenticationToken principal = githubPrincipal(12345);

        StudentUser student = new StudentUser();
        student.setGithubId("12345");
        student.setUsername("octocat");

        when(studentUserRepository.findByGithubId("12345")).thenReturn(Optional.of(student));

        var response = authController.getCurrentStudent(principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("octocat", response.getBody().getUsername());
    }

    @Test
    void returns404WhenGithubIdHasNoMatchingStudentRecord() {
        OAuth2AuthenticationToken principal = githubPrincipal(99999);

        when(studentUserRepository.findByGithubId("99999")).thenReturn(Optional.empty());

        var response = authController.getCurrentStudent(principal);

        assertEquals(404, response.getStatusCode().value());
    }

    private OAuth2AuthenticationToken githubPrincipal(int githubId) {
        Map<String, Object> attributes = Map.of("id", githubId, "login", "octocat");
        OAuth2User oAuth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, "id");
        return new OAuth2AuthenticationToken(oAuth2User, oAuth2User.getAuthorities(), "github");
    }
}
