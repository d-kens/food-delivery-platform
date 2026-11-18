# Notification Service

Sends out-of-app messages (email, SMS, push) through [Novu](https://novu.co). Live in-app updates are pushed by the Location Service over WebSocket.

It has no database, no REST API and no business rules. Content, channels, user preferences and delivery logs live in Novu.

## How it works

It consumes two topics:

- `user-events`: keeps Novu subscribers in sync with users (subscriber ID = user ID).
- `notifications`: triggers the Novu workflow for each notification `type`.

Invalid messages, and messages that still fail after retries, go to `<topic>.DLT`.

## User events

Topic `user-events`, key: user ID. Published by the User Service.

```json
{
  "eventId": "7f3c2a9e-5b1d-4e8a-9c0f-2d6b8e1a4f55",
  "eventType": "UserCreated",
  "occurredAt": "2026-11-18T09:00:00Z",
  "userId": "a1b2c3d4-0000-4000-8000-000000000001",
  "firstName": "Jane",
  "lastName": "Doe",
  "email": "jane@example.com",
  "phone": "+254700000000"
}
```

| Event | Action |
|---|---|
| `UserCreated`, `UserUpdated` | Create or update the subscriber (full user state) |
| `UserDeleted` | Delete the subscriber |

`eventId`, `eventType` and `userId` are required. Other event types are ignored.

## Notifications

Topic `notifications`, key: recipient ID.

```json
{
  "eventId": "3b1f9c2e-8a4d-4f6b-9e21-7c5d0a1b2c3d",
  "type": "UPDATED_PASSWORD",
  "recipientId": "a1b2c3d4-0000-4000-8000-000000000001",
  "payload": { "firstName": "Jane" }
}
```

`eventId`, `type` and `recipientId` are required. `payload` is passed to the Novu workflow.

| Type | Sent when |
|---|---|
| `UPDATED_PASSWORD` | A user changes their password |

## Configuration

| Variable | Description |
|---|---|
| `NOVU_SECRET` | Novu API secret key |
