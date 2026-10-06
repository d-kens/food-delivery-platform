# Notification Service

Turns events into out-of-app messages for users: simulated SMS, email and web push. Live in-app updates are pushed by the Location Service over WebSocket. It contains no business rules; it only formats and sends.

**Gateway prefix:** `/api/notifications`

## Responsibilities

- Listen to account and order events and pick the right template
- Send simulated SMS and email (logged) and real web push to the rider PWA
- Respect each user's notification preferences
- Keep a log of sent notifications

## Data it owns

`notification_db`: `templates`, `notification_log`, `preferences`, `push_subscriptions`, `processed_events`.

## Events

**Publishes**

- None

**Listens to**

- `UserRegistered` — welcome or "application received"
- `UserStatusChanged` — rider or account "you're approved" or "suspended"
- `RestaurantCreated` — "restaurant application received", sent to the owner
- `RestaurantStatusChanged` — "your restaurant is approved" or "suspended", sent to its owners
- `OrderPlaced / OrderAccepted / OrderPickedUp / OrderDelivered` — order progress messages
- `OrderCancelled / RefundCompleted` — cancellation and refund messages

## Calls other services

- User: `GET /internal/users/{id}` (contact details for any user)
