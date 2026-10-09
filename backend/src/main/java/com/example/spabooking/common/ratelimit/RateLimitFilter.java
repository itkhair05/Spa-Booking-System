package com.example.spabooking.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory fixed-window rate limiter for abuse-prone endpoints.
 * Keyed by client IP + rule; no external infrastructure required.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    public record Rule(String httpMethod, String pathPattern, int limit, long windowSeconds) {}

    private static final List<Rule> RULES = List.of(
            new Rule("POST", "/api/v1/auth/login", 10, 60),
            new Rule("POST", "/api/v1/auth/change-password", 10, 60),
            new Rule("POST", "/api/v1/public/spas/*/bookings", 20, 60),
            new Rule("GET", "/api/v1/public/spas/*/bookings/**", 30, 60),
            new Rule("POST", "/api/v1/bookings/*/refund", 10, 60),
            new Rule("POST", "/api/v1/public/spas/*/feedback", 10, 60)
    );

    private static final int CLEANUP_THRESHOLD = 10_000;
    private static final long STALE_WINDOW_AGE_SECONDS = 600;

    private final boolean enabled;
    private final ClientIpResolver clientIpResolver;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitFilter(boolean enabled) {
        this(enabled, new ClientIpResolver());
    }

    public RateLimitFilter(boolean enabled, ClientIpResolver clientIpResolver) {
        this.enabled = enabled;
        this.clientIpResolver = clientIpResolver != null ? clientIpResolver : new ClientIpResolver();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        Rule matched = matchRule(request);
        if (matched != null) {
            String clientIp = clientIpResolver.resolveClientIp(request);
            long retryAfterSeconds = consume(matched, clientIp);
            if (retryAfterSeconds > 0) {
                writeTooManyRequests(response, retryAfterSeconds);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Rule matchRule(HttpServletRequest request) {
        for (Rule rule : RULES) {
            if (rule.httpMethod().equals(request.getMethod())
                    && pathMatcher.match(rule.pathPattern(), request.getRequestURI())) {
                return rule;
            }
        }
        return null;
    }

    private long consume(Rule rule, String clientIp) {
        long now = java.time.Instant.now().getEpochSecond();
        long windowStart = (now / rule.windowSeconds()) * rule.windowSeconds();
        String key = rule.httpMethod() + " " + rule.pathPattern() + " " + clientIp;

        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.entrySet().removeIf(e -> e.getValue().windowStart < now - STALE_WINDOW_AGE_SECONDS);
        }

        Window window = windows.compute(key,
                (k, existing) -> (existing == null || existing.windowStart < windowStart)
                        ? new Window(windowStart)
                        : existing);

        if (window.count.incrementAndGet() <= rule.limit()) {
            return 0;
        }
        return windowStart + rule.windowSeconds() - now;
    }

    private void writeTooManyRequests(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 429);
        body.put("error", "Too Many Requests");
        body.put("message", "Too many requests. Please try again later.");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private static final class Window {
        final long windowStart;
        final AtomicInteger count = new AtomicInteger(0);

        Window(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
