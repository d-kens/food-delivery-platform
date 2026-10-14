package com.example.user_service.controller;

import com.example.user_service.dto.LoginRequestDto;
import com.example.user_service.dto.LoginResponseDto;
import com.example.user_service.dto.RegistrationRequestDto;
import com.example.user_service.dto.RegistrationResponseDto;
import com.example.user_service.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PostMapping("login")
    public LoginResponseDto login(
            @Valid @RequestBody final LoginRequestDto request
            ) {
        return authenticationService.login(request);
    }

    @PostMapping("register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponseDto register(
            @Valid @RequestBody final RegistrationRequestDto request
    ){
        return authenticationService.register(request);
    }
}
