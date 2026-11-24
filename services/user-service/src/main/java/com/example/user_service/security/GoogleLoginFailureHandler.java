package com.example.user_service.security;

import com.example.user_service.config.GoogleLoginProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleLoginFailureHandler implements AuthenticationFailureHandler {

    private final GoogleLoginProperties googleLoginProperties;

    // Called when Google login fails or the user cancels: drops the OAuth session and sends the user to the frontend login page with an error.
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.warn("Google login failed: {}", exception.getMessage(), exception);

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        response.sendRedirect(googleLoginProperties.failureUrl());
    }
}
