package com.example.user_service.exception;

public class UserSuspendedException extends RuntimeException {
    public UserSuspendedException(String message) {
        super(message);
    }
}
