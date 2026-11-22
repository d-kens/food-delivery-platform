package com.example.user_service.exception;

import org.springframework.http.HttpStatus;

public class UserDeletedException extends ApiException {
    public UserDeletedException() {
        super(HttpStatus.CONFLICT, "Account deleted", "This account has been deleted and cannot be changed");
    }
}
