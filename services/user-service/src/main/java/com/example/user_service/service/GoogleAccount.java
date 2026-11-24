package com.example.user_service.service;

public record GoogleAccount(
        String subject,
        String email,
        boolean emailVerified,
        String firstName,
        String lastName
) {
}
