package com.example.user_service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RegistrationResponseDto(
        UUID id,
        String email,
        String phoneNumber,
        String firstName,
        String lastName,
        List<String> roles,
        String status,
        Instant createdAt
) {
}