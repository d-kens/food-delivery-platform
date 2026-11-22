package com.example.user_service.dto;

import com.example.user_service.dto.validation.PasswordLength;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequestDto(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @PasswordLength
        String newPassword
) {
}
