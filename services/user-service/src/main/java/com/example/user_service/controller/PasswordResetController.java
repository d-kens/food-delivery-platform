package com.example.user_service.controller;

import com.example.user_service.dto.ForgotPasswordRequestDto;
import com.example.user_service.dto.ResetPasswordRequestDto;
import com.example.user_service.service.PasswordResetService;
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
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody final ForgotPasswordRequestDto request) {
        passwordResetService.requestReset(request.email());
    }

    @PostMapping("reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody final ResetPasswordRequestDto request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
    }
}
