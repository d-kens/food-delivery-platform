package com.example.user_service.dto;

import com.example.user_service.common.UserStatus;

import java.util.List;

public record UserResponseDto(
        String userId,
        String firstName,
        String lastName,
        String email,
        List<String> roles,
        String phoneNumber,
        UserStatus status
) {
}
