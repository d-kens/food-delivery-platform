package com.example.notification_service.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Recipient(String id, String firstName, String lastName, String email, String phone) {
}
