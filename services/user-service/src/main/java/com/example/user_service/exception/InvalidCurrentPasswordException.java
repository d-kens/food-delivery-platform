package com.example.user_service.exception;

import org.springframework.http.HttpStatus;

public class InvalidCurrentPasswordException extends ApiException {
    public InvalidCurrentPasswordException(String message) {
        super(HttpStatus.BAD_REQUEST, "Invalid current password", message);
    }
}
