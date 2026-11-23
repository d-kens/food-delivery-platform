package com.example.user_service.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends ApiException {
    public EmailAlreadyExistsException() {
        super(HttpStatus.CONFLICT, "Email already exists", "An account with this email already exists");
    }
}
