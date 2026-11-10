package com.example.user_service.service;

import com.example.user_service.dto.UserResponseDto;

public record RefreshResult(
        UserResponseDto user,
        String accessToken
) {
}
