package com.example.user_service.security;

import com.example.user_service.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AccessTokenCookieService {

    public static final String COOKIE_NAME = "access_token";

    private final JwtProperties jwtProperties;

    public ResponseCookie createAccessTokenCookie(String accessToken) {
        return buildCookie(accessToken, jwtProperties.expiration());
    }

    public ResponseCookie clearAccessTokenCookie() {
        return buildCookie("", Duration.ZERO);
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        return ResponseCookie
                .from(COOKIE_NAME, value)
                .path(jwtProperties.cookiePath())
                .maxAge(maxAge)
                .httpOnly(true)
                .secure(jwtProperties.cookieSecure())
                .sameSite("Strict")
                .build();
    }
}
