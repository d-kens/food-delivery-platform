package com.example.user_service.exception;

public class OwnStatusChangeException extends RuntimeException {
    public OwnStatusChangeException() {
        super("You cannot change your own account status");
    }
}
