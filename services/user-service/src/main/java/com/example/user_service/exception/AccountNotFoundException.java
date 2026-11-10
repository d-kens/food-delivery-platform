package com.example.user_service.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException() {
        super("This account no longer exists");
    }
}
