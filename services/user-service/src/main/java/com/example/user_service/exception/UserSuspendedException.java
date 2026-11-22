package com.example.user_service.exception;

import org.springframework.http.HttpStatus;

public class UserSuspendedException extends ApiException {
    public UserSuspendedException() {
        super(HttpStatus.FORBIDDEN, "Account Suspended", "Your account has been suspended");
    }
}
