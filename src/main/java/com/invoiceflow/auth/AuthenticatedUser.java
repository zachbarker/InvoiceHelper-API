package com.invoiceflow.auth;

import java.util.UUID;

/**
 * Minimal authenticated-principal representation. We deliberately don't
 * implement Spring Security's UserDetails/full user loading on every
 * request — the JWT itself carries enough (user ID) to identify the
 * caller without a DB hit, which is one of the main reasons to use JWTs
 * for the access token in the first place.
 */
public record AuthenticatedUser(UUID id, String email) {
}
