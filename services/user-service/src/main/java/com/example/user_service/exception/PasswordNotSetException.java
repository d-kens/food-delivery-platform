package com.example.user_service.exception;

import org.springframework.http.HttpStatus;

public class PasswordNotSetException extends ApiException {
    public PasswordNotSetException() {
        super(HttpStatus.BAD_REQUEST, "Password not set",
                "You don't have a password yet. Use 'Forgot password' to set one.");
    }
}
