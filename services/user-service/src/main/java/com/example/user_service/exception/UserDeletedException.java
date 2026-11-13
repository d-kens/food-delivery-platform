package com.example.user_service.exception;

public class UserDeletedException extends RuntimeException {
    public UserDeletedException() {
        super("This account has been deleted and cannot be changed");
    }
}
