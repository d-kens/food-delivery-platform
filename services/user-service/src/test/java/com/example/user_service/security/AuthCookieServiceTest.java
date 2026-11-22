package com.example.user_service.security;

import com.example.user_service.config.JwtProperties;
import com.example.user_service.config.RefreshTokenProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieServiceTest {

    private final AuthCookieService authCookieService = service(true, "/api", "/api/user-service/auth");

    @Test
    void accessTokenCookie_expiresWithTheTokenAndIsLockedDown() {
        ResponseCookie cookie = authCookieService.accessTokenCookie("access-token");

        assertThat(cookie.getName()).isEqualTo(AuthCookieService.ACCESS_TOKEN_COOKIE);
        assertThat(cookie.getValue()).isEqualTo("access-token");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofMinutes(15));
        assertThat(cookie.getPath()).isEqualTo("/api");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
    }

    @Test
    void refreshTokenCookie_expiresWithTheRefreshTtlAndIsLockedDown() {
        ResponseCookie cookie = authCookieService.refreshTokenCookie("refresh-token");

        assertThat(cookie.getName()).isEqualTo(AuthCookieService.REFRESH_TOKEN_COOKIE);
        assertThat(cookie.getValue()).isEqualTo("refresh-token");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(7));
        assertThat(cookie.getPath()).isEqualTo("/api/user-service/auth");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
    }

    @Test
    void clearedCookies_expiresBothCookiesOnTheirOwnPaths() {
        assertThat(authCookieService.clearedCookies()).satisfiesExactly(
                access -> assertThat(access)
                        .startsWith("access_token=;")
                        .contains("Path=/api;", "Max-Age=0", "Secure", "HttpOnly", "SameSite=Strict"),
                refresh -> assertThat(refresh)
                        .startsWith("refresh_token=;")
                        .contains("Path=/api/user-service/auth;", "Max-Age=0", "Secure", "HttpOnly", "SameSite=Strict"));
    }

    @Test
    void cookies_useConfiguredSecureFlagAndPaths() {
        var devService = service(false, "/", "/auth");

        assertThat(devService.accessTokenCookie("a").isSecure()).isFalse();
        assertThat(devService.accessTokenCookie("a").getPath()).isEqualTo("/");
        assertThat(devService.refreshTokenCookie("r").isSecure()).isFalse();
        assertThat(devService.refreshTokenCookie("r").getPath()).isEqualTo("/auth");
    }

    private static AuthCookieService service(boolean secure, String accessPath, String refreshPath) {
        return new AuthCookieService(
                new JwtProperties("issuer", "key-id", Duration.ofMinutes(15), null, null, secure, accessPath),
                new RefreshTokenProperties(Duration.ofDays(7), secure, refreshPath));
    }
}
