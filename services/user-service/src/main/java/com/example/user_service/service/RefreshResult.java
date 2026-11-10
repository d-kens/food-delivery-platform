package com.example.user_service.service;

import com.example.user_service.dto.AuthResponseDto;

public record RefreshResult(
        AuthResponseDto authResponse,
        String accessToken
) {
}
