package com.invoiceflow.auth;

import com.invoiceflow.common.config.JwtProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh tokens are opaque random strings (not JWTs). Only a SHA-256 hash
 * is persisted, so a database leak doesn't expose usable tokens. Storing
 * them (rather than using a stateless JWT refresh token) is what lets us
 * revoke a single session or all of a user's sessions on demand.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    /** Issues a new refresh token for the user and returns the raw (unhashed) value. */
    public String issue(UUID userId) {
        String rawToken = generateRawToken();

        RefreshToken entity = RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash(rawToken))
                .expiresAt(Instant.now().plus(jwtProperties.refreshTokenTtlDays(), ChronoUnit.DAYS))
                .revoked(false)
                .build();

        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /**
     * Validates a raw refresh token. If valid, revokes it and issues a
     * replacement (rotation) — this limits how long a stolen refresh token
     * stays usable, and a replay of an already-rotated token can be
     * detected as a signal of compromise.
     */
    public Optional<RotationResult> rotate(String rawToken) {
        return refreshTokenRepository.findByTokenHash(hash(rawToken))
                .filter(token -> !token.isRevoked())
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
                .map(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                    String newRawToken = issue(token.getUserId());
                    return new RotationResult(token.getUserId(), newRawToken);
                });
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.findByUserIdAndRevokedFalse(userId)
                .forEach(t -> t.setRevoked(true));
        // Dirty-checked entities are flushed automatically at transaction
        // commit; callers should invoke this within a @Transactional method.
    }

    private String generateRawToken() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes());
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record RotationResult(UUID userId, String newRawToken) {
    }
}
