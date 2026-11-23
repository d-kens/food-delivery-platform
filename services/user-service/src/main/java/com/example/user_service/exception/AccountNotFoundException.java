package com.example.user_service.exception;

import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends ApiException {
    public AccountNotFoundException() {
        super(HttpStatus.UNAUTHORIZED, "Account Not Found", "This account no longer exists");
    }
}
