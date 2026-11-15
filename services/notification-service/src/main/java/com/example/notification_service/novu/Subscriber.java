package com.example.notification_service.novu;

public record Subscriber(
        String id,
        String firstName,
        String lastName,
        String email,
        String phone
) {
}
