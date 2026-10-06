# Notification Service

Turns events into messages for users: simulated SMS, email and web push. It contains no business rules; it only formats and sends.

**Gateway prefix:** `/api/notifications`

## Responsibilities

- Listen to account and order events and pick the right template
- Send simulated SMS and email (logged) and real web push to the rider PWA
- Respect each user's notification preferences
- Keep a log of sent notifications

## Data it owns

`notification_db`: `templates`, `notification_log`, `preferences`, `push_subscriptions`, `processed_events`.

## Endpoints

| Method | Path | Access | Description |
|---|---|---|---|
| `GET` | `/api/notifications/me` | Authenticated | My recent notifications |
| `PATCH` | `/api/notifications/me/{id}/read` | Authenticated (owner) | Mark a notification as read |
| `GET` | `/api/notifications/me/preferences` | Authenticated | My channel preferences |
| `PUT` | `/api/notifications/me/preferences` | Authenticated | Update my channel preferences |
| `POST` | `/api/notifications/me/push-subscriptions` | Authenticated | Register this browser for web push |
| `DELETE` | `/api/notifications/me/push-subscriptions/{id}` | Authenticated (owner) | Remove a push subscription |
| `GET` | `/api/notifications/admin/templates` | ADMIN | List templates |
| `PUT` | `/api/notifications/admin/templates/{id}` | ADMIN | Edit a template |

Paths starting with `/internal` are for other services only. The gateway does not route them.

## Events

**Publishes**

- None

**Listens to**

- `UserRegistered` — welcome or "application received"
- `UserStatusChanged` — "you're approved" or "suspended"
- `OrderPlaced / OrderAccepted / OrderPickedUp / OrderDelivered` — order progress messages
- `OrderCancelled / RefundCompleted` — cancellation and refund messages

## Calls other services

- Customer: `GET /internal/customers/{id}` (contact details)
