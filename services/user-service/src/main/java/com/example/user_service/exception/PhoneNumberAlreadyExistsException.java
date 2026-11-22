package com.example.user_service.exception;

import org.springframework.http.HttpStatus;

public class PhoneNumberAlreadyExistsException extends ApiException {
    public PhoneNumberAlreadyExistsException() {
        super(HttpStatus.CONFLICT, "Phone number already exists", "An account with this phone number already exists");
    }
}
