package com.example.user_service.controller;

import com.example.user_service.dto.AuthResponseDto;
import com.example.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @GetMapping("me")
    public AuthResponseDto me(@AuthenticationPrincipal final Jwt jwt) {
        return userService.getCurrentUser(UUID.fromString(jwt.getSubject()));
    }
}
