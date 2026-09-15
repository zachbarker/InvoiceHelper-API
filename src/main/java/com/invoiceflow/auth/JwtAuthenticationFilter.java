package com.invoiceflow.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        extractBearerToken(request)
                .flatMap(jwtService::validateAndGetUserId)
                .ifPresent(userId -> authenticate(userId, request));

        filterChain.doFilter(request, response);
    }

    private void authenticate(UUID userId, HttpServletRequest request) {
        // Email isn't needed for authorization decisions here, so we don't
        // decode it separately; controllers needing full user data load it
        // from the repository via the ID.
        AuthenticatedUser principal = new AuthenticatedUser(userId, null);

        var authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        authentication.setDetails(request);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Optional<String> extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }
}
