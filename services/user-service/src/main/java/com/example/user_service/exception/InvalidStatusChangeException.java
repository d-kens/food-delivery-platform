package com.example.user_service.exception;

import org.springframework.http.HttpStatus;

public class InvalidStatusChangeException extends ApiException {
    public InvalidStatusChangeException(String message) {
        super(HttpStatus.BAD_REQUEST, "Invalid status change", message);
    }
}
