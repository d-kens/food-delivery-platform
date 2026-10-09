package com.example.user_service.dto;

import java.time.Instant;
import java.util.UUID;

public record LoginResponse(
        String accessToken,
        long accessTokenTtl,
        Instant accessTokenExpiration,
        UserDetails user
) {
    public record UserDetails(
            UUID id,
            String email,
            String phoneNumber,
            String firstName,
            String lastName,
            String role,
            String status
    ) {
    }
}
