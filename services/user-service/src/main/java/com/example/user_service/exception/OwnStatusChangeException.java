package com.example.user_service.exception;

import org.springframework.http.HttpStatus;

public class OwnStatusChangeException extends ApiException {
    public OwnStatusChangeException() {
        super(HttpStatus.CONFLICT, "Cannot change own status", "You cannot change your own account status");
    }
}
