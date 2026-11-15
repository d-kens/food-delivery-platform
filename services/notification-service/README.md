# Notification Service

Turns domain events into out-of-app messages (email, SMS, push) through [Novu](https://novu.co). Live in-app updates are pushed by the Location Service over WebSocket.

It has no database, no REST API and no business rules. Content, channels, user preferences and delivery logs live in Novu.

## How it works

1. Consumes events from Kafka.
2. User events keep Novu subscribers in sync (subscriber ID = user ID).
3. Other events trigger the matching Novu workflow for the user.

## Events

**Publishes**

- None

**Listens to** (`user-events`)

| Event | Action |
|---|---|
| `UserRegistered` | Create subscriber, then `welcome` |
| `UserUpdated` | Update subscriber |
| `UserStatusChanged` | `account-status-changed` |
| `PasswordResetRequested` | `password-reset` |
| `PasswordChanged` | `password-changed` |

Workflow IDs must exist in Novu. Order, restaurant and payment events are added as those services are built.

## Configuration

| Variable | Description |
|---|---|
| `NOVU_SECRET` | Novu API secret key |
