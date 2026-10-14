package com.example.user_service.dto;

public record LoginResponseDto(
        String accessToken,
        String userId,
        String firstName,
        String lastName,
        String email,
        String role
) {
}
