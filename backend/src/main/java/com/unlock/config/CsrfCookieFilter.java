package com.unlock.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security's CSRF token is generated "lazily" - it only actually
 * gets written into a response cookie once something reads it. Without
 * this filter, the XSRF-TOKEN cookie the frontend needs would never
 * appear until some code happened to touch the token, which is
 * unreliable. This filter just forces that read on every request, so
 * the frontend can always count on the cookie being there.
 *
 * This is the standard pattern recommended in Spring Security's own
 * docs for cookie-based CSRF with a separate frontend.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken(); // touching this is what triggers the cookie to be written
        }
        filterChain.doFilter(request, response);
    }
}
