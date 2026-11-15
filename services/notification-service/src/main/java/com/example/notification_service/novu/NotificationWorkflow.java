package com.example.notification_service.novu;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationWorkflow {
    WELCOME("welcome"),
    ACCOUNT_STATUS_CHANGED("account-status-changed"),
    PASSWORD_RESET("password-reset"),
    PASSWORD_CHANGED("password-changed");

    private final String workflowId;
}
