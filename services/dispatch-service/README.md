# Dispatch Service

Owns the rider's working details (vehicle, availability, rating) and matches riders to orders. Tracks which riders are online and available, offers jobs to the nearest one, and moves on if they decline or don't answer.

**Gateway prefix:** `/api/dispatch`

## Responsibilities

- Create a rider record (vehicle details) as `PENDING` when the User Service registers a rider; name and phone live in the User Service
- Only let `ACTIVE` riders go online: approval and suspension arrive through `UserStatusChanged`
- Track rider availability: `OFFLINE`, `AVAILABLE`, `BUSY`
- When an order is accepted, ask Location for the nearest riders and keep only those that are `AVAILABLE` here (Location knows positions, Dispatch knows availability)
- Offer the job to one rider at a time with a 30-second timeout; a scheduled job expires overdue offers and moves to the next rider (Redis key expiry alone is not reliable for this)
- Give up after 15 minutes and publish `NoRiderFound`
- Free the rider when the order is delivered or cancelled
- Keep each rider's average rating up to date from `OrderRated` events

## Data it owns

`dispatch_db`: `riders` (user ID, status, vehicle, availability, average rating, rating count), `assignments`, `offers` (history), `processed_events`, `outbox`. Redis: active offers with their expiry time.

## Events

**Publishes**

- `RiderOfferCreated`
- `RiderOfferExpired`
- `RiderAssigned`
- `NoRiderFound`

**Listens to**

- `UserStatusChanged` — mark riders `ACTIVE` when approved; take suspended riders offline and block them
- `OrderAccepted` — start matching, using the pickup and drop-off coordinates in the event
- `OrderDelivered` — free the rider
- `OrderCancelled` — stop matching or free the rider
- `OrderRated` — update the rider's average rating

## Calls other services

- Location: `GET /internal/riders/nearby`
