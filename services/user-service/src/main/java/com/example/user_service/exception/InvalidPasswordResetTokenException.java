package com.example.user_service.exception;

import org.springframework.http.HttpStatus;

public class InvalidPasswordResetTokenException extends ApiException {
    public InvalidPasswordResetTokenException() {
        super(HttpStatus.BAD_REQUEST, "Invalid reset link", "This reset link is invalid or has expired");
    }
}
