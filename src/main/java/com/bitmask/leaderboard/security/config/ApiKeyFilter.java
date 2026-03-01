package com.bitmask.leaderboard.security.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@AllArgsConstructor
public final class ApiKeyFilter extends OncePerRequestFilter {
    private static final String PREFIX = "ApiKey ";
    private static final String AUTH_HEADER_PREFIX = "Authorization";
    private final String validApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader(AUTH_HEADER_PREFIX);

        if (authHeader != null && authHeader.startsWith(PREFIX)) {
            String requestKey = authHeader.substring(PREFIX.length());

            if (validApiKey.equals(requestKey)) {
                var auth = new UsernamePasswordAuthenticationToken(
                        "server",
                        null,
                        List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
