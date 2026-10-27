package com.example.user_service.dto;

public record AuthResponseDto(
        String accessToken,
        String userId,
        String firstName,
        String lastName,
        String email,
        String role
) {
}
