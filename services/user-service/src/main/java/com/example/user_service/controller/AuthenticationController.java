package com.example.user_service.controller;

import com.example.user_service.dto.AuthRequestDto;
import com.example.user_service.dto.UserResponseDto;
import com.example.user_service.exception.InvalidRefreshTokenException;
import com.example.user_service.dto.RegistrationRequestDto;
import com.example.user_service.dto.RegistrationResponseDto;
import com.example.user_service.security.AccessTokenCookieService;
import com.example.user_service.security.RefreshTokenCookieService;
import com.example.user_service.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final AccessTokenCookieService accessTokenCookieService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    @PostMapping("register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponseDto register(
            @Valid @RequestBody final RegistrationRequestDto request
    ){
        return authenticationService.register(request);
    }

    @PostMapping("login")
    public ResponseEntity<UserResponseDto> login(
            @Valid @RequestBody final AuthRequestDto request
    ) {
        var loginResult = authenticationService.login(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        accessTokenCookieService.createAccessTokenCookie(loginResult.accessToken()).toString(),
                        refreshTokenCookieService.createRefreshTokenCookie(loginResult.refreshToken()).toString())
                .body(loginResult.user());
    }

    @PostMapping("refresh-token")
    public ResponseEntity<UserResponseDto> refreshToken(
            @CookieValue(name = RefreshTokenCookieService.COOKIE_NAME, required = false) final String refreshToken
    ) {
        var refreshResult = authenticationService.refreshToken(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        accessTokenCookieService.createAccessTokenCookie(refreshResult.accessToken()).toString())
                .body(refreshResult.user());
    }

    @PostMapping("logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenCookieService.COOKIE_NAME, required = false) final String refreshToken
    ) {
        authenticationService.logout(refreshToken);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearedAuthCookies())
                .build();
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRefreshToken(final InvalidRefreshTokenException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Invalid Refresh Token");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.SET_COOKIE, clearedAuthCookies())
                .body(problem);
    }

    private String[] clearedAuthCookies() {
        return new String[] {
                accessTokenCookieService.clearAccessTokenCookie().toString(),
                refreshTokenCookieService.clearRefreshTokenCookie().toString()
        };
    }
}
