package com.example.user_service.controller;

import com.example.user_service.dto.AuthRequestDto;
import com.example.user_service.dto.AuthResponseDto;
import com.example.user_service.dto.RegistrationRequestDto;
import com.example.user_service.dto.RegistrationResponseDto;
import com.example.user_service.security.RefreshTokenCookieService;
import com.example.user_service.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.HttpHeaders;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    @PostMapping("login")
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody final AuthRequestDto request
    ) {
        var loginResult = authenticationService.login(request);

        var authResponse = loginResult.authResponse();
        var refreshToken = loginResult.refreshToken();

        ResponseCookie refreshCookie = refreshTokenCookieService.createRefreshTokenCookie(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(authResponse);
    }

    @PostMapping("register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponseDto register(
            @Valid @RequestBody final RegistrationRequestDto request
    ){
        return authenticationService.register(request);
    }
}
