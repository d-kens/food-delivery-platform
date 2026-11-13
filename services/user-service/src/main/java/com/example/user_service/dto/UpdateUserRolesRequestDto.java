package com.example.user_service.dto;

import com.example.user_service.common.Role;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdateUserRolesRequestDto(
        @NotEmpty(message = "At least one role is required")
        Set<@NotNull(message = "Role must not be null") Role> roles
) {
}
