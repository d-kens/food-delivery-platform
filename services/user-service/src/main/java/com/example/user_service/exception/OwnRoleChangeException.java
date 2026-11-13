package com.example.user_service.exception;

public class OwnRoleChangeException extends RuntimeException {
    public OwnRoleChangeException() {
        super("You cannot change your own roles");
    }
}
