package com.example.user_service.service;

import com.example.user_service.dto.AuthResponseDto;

public record LoginResult(
        AuthResponseDto authResponse,
        String refreshToken
) {
}
