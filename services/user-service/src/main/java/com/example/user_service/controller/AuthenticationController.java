package com.example.user_service.controller;

import com.example.user_service.dto.RegistrationRequest;
import com.example.user_service.dto.RegistrationResponse;
import com.example.user_service.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("register")
    public RegistrationResponse register(
            @Valid @RequestBody RegistrationRequest request
    ){
        return authenticationService.register(request);
    }
}
