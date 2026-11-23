package com.example.user_service.security;

import com.example.user_service.config.JwtProperties;
import com.example.user_service.config.RefreshTokenProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AuthCookieService {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final JwtProperties jwtProperties;
    private final RefreshTokenProperties refreshTokenProperties;

    public ResponseCookie accessTokenCookie(String accessToken) {
        return accessCookie(accessToken, jwtProperties.expiration());
    }

    public ResponseCookie refreshTokenCookie(String refreshToken) {
        return refreshCookie(refreshToken, refreshTokenProperties.ttl());
    }

    public String[] clearedCookies() {
        return new String[] {
                accessCookie("", Duration.ZERO).toString(),
                refreshCookie("", Duration.ZERO).toString()
        };
    }

    private ResponseCookie accessCookie(String value, Duration maxAge) {
        return build(ACCESS_TOKEN_COOKIE, value, maxAge, jwtProperties.cookiePath(), jwtProperties.cookieSecure());
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return build(REFRESH_TOKEN_COOKIE, value, maxAge,
                refreshTokenProperties.cookiePath(), refreshTokenProperties.cookieSecure());
    }

    private static ResponseCookie build(String name, String value, Duration maxAge, String path, boolean secure) {
        return ResponseCookie
                .from(name, value)
                .path(path)
                .maxAge(maxAge)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .build();
    }
}
