package com.example.user_service.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmAccountActionRequestDto(
        @NotBlank(message = "Token is required")
        String token
) {
}
