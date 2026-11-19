package com.example.user_service.security;

import com.example.user_service.config.RefreshTokenProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenCookieServiceTest {

    private final RefreshTokenCookieService refreshTokenCookieService = new RefreshTokenCookieService(
            new RefreshTokenProperties(Duration.ofDays(7), true, "/api/user-service/auth"));

    @Test
    void createRefreshTokenCookie_returnsCookieWithTokenAndSecureAttributes() {
        ResponseCookie cookie = refreshTokenCookieService.createRefreshTokenCookie("refresh-token");

        assertThat(cookie.getName()).isEqualTo(RefreshTokenCookieService.COOKIE_NAME);
        assertThat(cookie.getValue()).isEqualTo("refresh-token");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(7));
        assertThat(cookie.getPath()).isEqualTo("/api/user-service/auth");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
    }

    @Test
    void clearRefreshTokenCookie_returnsEmptyExpiredCookieWithSameAttributes() {
        ResponseCookie cookie = refreshTokenCookieService.clearRefreshTokenCookie();

        assertThat(cookie.getName()).isEqualTo(RefreshTokenCookieService.COOKIE_NAME);
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
        assertThat(cookie.getPath()).isEqualTo("/api/user-service/auth");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
    }

    @Test
    void createRefreshTokenCookie_usesConfiguredSecureAndPath() {
        var devCookieService = new RefreshTokenCookieService(
                new RefreshTokenProperties(Duration.ofDays(7), false, "/auth"));

        ResponseCookie cookie = devCookieService.createRefreshTokenCookie("refresh-token");

        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.getPath()).isEqualTo("/auth");
    }
}
