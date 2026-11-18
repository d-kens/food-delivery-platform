package com.example.user_service.exception;

public class InvalidPasswordResetTokenException extends RuntimeException {
    public InvalidPasswordResetTokenException() {
        super("This reset link is invalid or has expired");
    }
}
