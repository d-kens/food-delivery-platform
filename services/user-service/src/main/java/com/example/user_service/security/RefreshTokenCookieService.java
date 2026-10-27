package com.example.user_service.security;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshTokenCookieService {

    @Value("${security.refresh-token.ttl}")
    private Duration refreshTokenTtl;

    @Value("${security.refresh-token.cookie-secure}")
    private boolean cookieSecure;

    @Value("${security.refresh-token.cookie-path}")
    private String cookiePath;

    public static final String COOKIE_NAME = "refresh_token";

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return buildCookie(refreshToken, refreshTokenTtl);
    }

    public ResponseCookie clearRefreshTokenCookie() {
        return buildCookie("", Duration.ZERO);
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        return ResponseCookie
                .from(COOKIE_NAME, value)
                .path(cookiePath)
                .maxAge(maxAge)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .build();
    }
}
