package com.example.user_service.service;

import com.example.user_service.dto.LoginResponse;

import java.time.Duration;

public record LoginResult(
        LoginResponse response,
        String refreshToken,
        Duration refreshTokenTtl
) {
}
