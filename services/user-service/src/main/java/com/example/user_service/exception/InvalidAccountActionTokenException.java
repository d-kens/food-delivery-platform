package com.example.user_service.exception;

import org.springframework.http.HttpStatus;

public class InvalidAccountActionTokenException extends ApiException {
    public InvalidAccountActionTokenException() {
        super(HttpStatus.BAD_REQUEST, "Invalid link", "This link is invalid or has expired");
    }
}
