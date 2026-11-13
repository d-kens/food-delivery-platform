package com.example.user_service.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequestDto(
        @NotBlank(message = "Current password is required")
        String currentPassword
) {
}
