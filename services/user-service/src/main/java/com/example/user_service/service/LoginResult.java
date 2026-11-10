package com.example.user_service.service;

import com.example.user_service.dto.UserResponseDto;

public record LoginResult(
        UserResponseDto user,
        String accessToken,
        String refreshToken
) {
}
