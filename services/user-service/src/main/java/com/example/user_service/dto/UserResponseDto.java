package com.example.user_service.dto;

import java.util.List;

public record UserResponseDto(
        String userId,
        String firstName,
        String lastName,
        String email,
        List<String> roles
) {
}
