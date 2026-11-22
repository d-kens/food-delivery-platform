package com.example.user_service.dto;

import com.example.user_service.dto.validation.PasswordLength;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequestDto(
        @NotBlank(message = "Reset token is required")
        String token,

        @NotBlank(message = "New password is required")
        @PasswordLength
        String newPassword
) {
}
