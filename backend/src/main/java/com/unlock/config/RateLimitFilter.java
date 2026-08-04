package com.unlock.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple, dependency-free rate limiter for the endpoints that do real
 * work (GitHub API calls, pattern scanning) - so a bug or an abusive
 * script can't hammer these repeatedly. This is intentionally basic:
 * a fixed number of requests per session, per minute, tracked in memory.
 *
 * Good enough for a small deployment; if this app ever runs on more
 * than one server instance, this in-memory approach would need to move
 * to something shared like Redis instead.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_WINDOW = 10;
    private static final long WINDOW_SECONDS = 60;

    // Only the "expensive" endpoints need protecting - reads (GET) are cheap.
    private static final List<String> LIMITED_PATHS = List.of(
            "/api/entries/sync",
            "/api/patterns/scan"
    );

    private final Map<String, RequestWindow> windowsBySession = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        boolean isLimitedPath = LIMITED_PATHS.stream().anyMatch(path -> request.getRequestURI().endsWith(path));

        if (isLimitedPath && request.getSession(false) != null) {
            String sessionId = request.getSession().getId();
            RequestWindow window = windowsBySession.computeIfAbsent(sessionId, k -> new RequestWindow());

            if (window.isOverLimit()) {
                response.setStatus(429); // Too Many Requests
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"message\":\"You're doing that a bit too often - give it a minute and try again.\"}"
                );
                return;
            }
            window.recordRequest();
        }

        filterChain.doFilter(request, response);
    }

    /** Tracks request timestamps for one session within a rolling time window. */
    private static class RequestWindow {
        private Instant windowStart = Instant.now();
        private int count = 0;

        synchronized boolean isOverLimit() {
            resetIfWindowExpired();
            return count >= MAX_REQUESTS_PER_WINDOW;
        }

        synchronized void recordRequest() {
            resetIfWindowExpired();
            count++;
        }

        private void resetIfWindowExpired() {
            if (Instant.now().isAfter(windowStart.plusSeconds(WINDOW_SECONDS))) {
                windowStart = Instant.now();
                count = 0;
            }
        }
    }
}
