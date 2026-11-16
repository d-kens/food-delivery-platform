package com.example.notification_service.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    WELCOME("welcome");

    private final String workflowId;
}
