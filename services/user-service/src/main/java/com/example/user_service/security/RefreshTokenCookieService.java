package com.example.user_service.security;

import com.example.user_service.config.RefreshTokenProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieService {

    public static final String COOKIE_NAME = "refresh_token";

    private final RefreshTokenProperties properties;

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return buildCookie(refreshToken, properties.ttl());
    }

    public ResponseCookie clearRefreshTokenCookie() {
        return buildCookie("", Duration.ZERO);
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        return ResponseCookie
                .from(COOKIE_NAME, value)
                .path(properties.cookiePath())
                .maxAge(maxAge)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Strict")
                .build();
    }
}
