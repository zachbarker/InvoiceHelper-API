package com.invoiceflow.auth;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        UUID userId,
        String email,
        String name,
        UUID organizationId,
        String organizationRole
) {
}
