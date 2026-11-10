package com.example.user_service.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.util.WebUtils;

public class CookieBearerTokenResolver implements BearerTokenResolver {

    private final DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();
    private final RequestMatcher publicEndpoints;

    public CookieBearerTokenResolver(RequestMatcher publicEndpoints) {
        this.publicEndpoints = publicEndpoints;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        if (publicEndpoints.matches(request)) {
            return null;
        }

        String headerToken = headerResolver.resolve(request);
        if (headerToken != null) {
            return headerToken;
        }

        Cookie cookie = WebUtils.getCookie(request, AccessTokenCookieService.COOKIE_NAME);
        return cookie == null || cookie.getValue().isBlank() ? null : cookie.getValue();
    }
}
