package com.example.user_service.dto;

import java.time.Instant;
import java.util.UUID;

public record RegistrationResponse(
        UUID id,
        String email,
        String phoneNumber,
        String firstName,
        String lastName,
        String role,
        String status,
        Instant createdAt
) {
}