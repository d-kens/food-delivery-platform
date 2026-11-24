package com.example.notification_service.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    UPDATED_PASSWORD("UPDATED_PASSWORD"),
    RESET_PASSWORD("RESET_PASSWORD"),
    CONFIRM_ACCOUNT_DELETION("CONFIRM_ACCOUNT_DELETION"),
    CONFIRM_EMAIL_CHANGE("CONFIRM_EMAIL_CHANGE");

    private final String workflowId;
}