package com.example.user_service.security;

import com.example.user_service.config.GoogleLoginProperties;
import com.example.user_service.exception.ApiException;
import com.example.user_service.service.AuthenticationService;
import com.example.user_service.service.GoogleAccount;
import com.example.user_service.service.LoginResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final GoogleLoginProperties googleLoginProperties;
    private final AuthenticationService authenticationService;
    private final AuthCookieService authCookieService;

    /*
     * Called after Spring has validated Google's response. Finds, links or creates our user, sets the same
     * access/refresh cookies as /auth/login and sends the browser to the frontend. If we refuse the login
     * (suspended, unverified email, email linked to another Google account) it goes to the failure URL instead.
     * The session only held the OAuth state, so it is dropped either way.
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        invalidateSession(request);

        OidcUser googleUser = (OidcUser) authentication.getPrincipal();

        LoginResult loginResult;
        try {
            loginResult = authenticationService.loginWithGoogle(toGoogleAccount(googleUser));
        } catch (ApiException exception) {
            log.warn("Google login refused: {}", exception.getClass().getSimpleName());
            response.sendRedirect(googleLoginProperties.failureUrl());
            return;
        }

        response.addHeader(HttpHeaders.SET_COOKIE, authCookieService.accessTokenCookie(loginResult.accessToken()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, authCookieService.refreshTokenCookie(loginResult.refreshToken()).toString());
        response.sendRedirect(googleLoginProperties.successUrl());
    }

    private GoogleAccount toGoogleAccount(OidcUser googleUser) {
        return new GoogleAccount(
                googleUser.getSubject(),
                googleUser.getEmail(),
                Boolean.TRUE.equals(googleUser.getEmailVerified()),
                googleUser.getGivenName(),
                googleUser.getFamilyName()
        );
    }

    private void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
