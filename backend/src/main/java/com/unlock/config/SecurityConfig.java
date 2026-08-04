package com.unlock.config;

import com.unlock.service.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final RateLimitFilter rateLimitFilter;

    // Where your plain HTML/JS frontend runs during local development.
    // Change this if you serve the frontend from a different port.
    private static final String FRONTEND_URL = "http://localhost:5500";

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService, RateLimitFilter rateLimitFilter) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Cookie-based CSRF: the browser gets a readable XSRF-TOKEN cookie,
        // and the frontend must echo it back in an X-XSRF-TOKEN header on
        // every state-changing request (POST/PUT/DELETE). GET requests
        // don't need it. This is the standard pattern Spring Security
        // recommends for a separate frontend + backend setup like ours.
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(requestHandler)
            )
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
            .addFilterAfter(rateLimitFilter, CsrfCookieFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/api/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                // After a successful login, send the browser back to the frontend dashboard
                .defaultSuccessUrl(FRONTEND_URL + "/dashboard", true)
            )
            .logout(logout -> logout
                .logoutSuccessUrl(FRONTEND_URL + "/")
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(FRONTEND_URL));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(List.of("*"));
        // Required so the browser sends the login session cookie along with API requests
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
