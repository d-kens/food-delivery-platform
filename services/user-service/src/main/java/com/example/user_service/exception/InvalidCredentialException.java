package com.example.user_service.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialException extends ApiException {
    public InvalidCredentialException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid Credentials", "Invalid email or password");
    }
}
