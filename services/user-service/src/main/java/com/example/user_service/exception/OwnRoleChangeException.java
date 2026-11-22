package com.example.user_service.exception;

import org.springframework.http.HttpStatus;

public class OwnRoleChangeException extends ApiException {
    public OwnRoleChangeException() {
        super(HttpStatus.CONFLICT, "Cannot change own roles", "You cannot change your own roles");
    }
}
