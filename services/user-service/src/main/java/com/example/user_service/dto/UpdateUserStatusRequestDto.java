package com.example.user_service.dto;

import com.example.user_service.common.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequestDto(
        @NotNull(message = "Status is required")
        UserStatus status
) {
}
