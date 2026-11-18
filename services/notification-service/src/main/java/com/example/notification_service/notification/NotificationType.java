package com.example.notification_service.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    UPDATED_PASSWORD("UPDATED_PASSWORD"),
    RESET_PASSWORD("RESET_PASSWORD");

    private final String workflowId;
}