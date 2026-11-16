# Notification Service

Sends out-of-app messages (email, SMS, push) through [Novu](https://novu.co). Live in-app updates are pushed by the Location Service over WebSocket.

It has no database, no REST API and no business rules. Content, channels, user preferences and delivery logs live in Novu.

## How it works

1. Other services publish notification requests to the `notifications` topic.
2. The service creates or updates the recipient as a Novu subscriber (subscriber ID = user ID).
3. It triggers the Novu workflow for the notification `type`.

Invalid messages, and messages that still fail after retries, go to `notifications.notification-service.DLT`.

## Message

Key: recipient ID. Value:

```json
{
  "eventId": "7f3c2a9e-5b1d-4e8a-9c0f-2d6b8e1a4f55",
  "type": "WELCOME",
  "recipient": {
    "id": "a1b2c3d4-0000-4000-8000-000000000001",
    "firstName": "Jane",
    "lastName": "Doe",
    "email": "jane@example.com",
    "phone": "+254700000000"
  },
  "data": { "firstName": "Jane" }
}
```

`eventId`, `type` and `recipient.id` are required. `data` is passed to the template.

| Type | Sent when |
|---|---|
| `WELCOME` | A user signs up |

## Configuration

| Variable | Description |
|---|---|
| `NOVU_SECRET` | Novu API secret key |
