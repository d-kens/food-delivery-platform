# Dispatch Service

Matches riders to orders. Tracks which riders are online and available, offers jobs to the nearest one, and moves on if they decline or don't answer.

**Gateway prefix:** `/api/dispatch`

## Responsibilities

- Create a rider profile when a rider registers
- Track rider availability: `OFFLINE`, `AVAILABLE`, `BUSY`
- When an order is accepted, ask Location for the nearest available riders
- Offer the job to one rider at a time with a 30-second timeout (stored in Redis)
- Give up after 15 minutes and publish `NoRiderFound`
- Free the rider when the order is delivered or cancelled

## Data it owns

`dispatch_db`: `riders` (user ID, vehicle, availability), `assignments`, `offers` (history), `processed_events`, `outbox`. Redis: active offers with TTL.

## Endpoints

| Method | Path | Access | Description |
|---|---|---|---|
| `GET` | `/api/dispatch/{id}` | RIDER | My rider profile and availability |
| `PUT` | `/api/dispatch/{id}` | RIDER | Update vehicle details |
| `POST` | `/api/dispatch/{id}/online` | RIDER | Go online and become available |
| `POST` | `/api/dispatch/{id}/offline` | RIDER | Go offline |
| `GET` | `/api/dispatch/{id}/offers/current` | RIDER | Current job offer, if any |
| `POST` | `/api/dispatch/offers/{id}/accept` | RIDER (offered) | Accept a job offer |
| `POST` | `/api/dispatch/offers/{id}/decline` | RIDER (offered) | Decline a job offer |
| `GET` | `/api/dispatch/{id}/assignments` | RIDER | My delivery history |
| `GET` | `/api/dispatch/{id}/assignments/current` | RIDER | My active delivery |
| `GET` | `/api/dispatch/admin/riders` | ADMIN | List riders, filter by availability |
| `GET` | `/api/dispatch/admin/assignments` | ADMIN | Assignments, filter by order or rider |

Paths starting with `/internal` are for other services only. The gateway does not route them.

## Events

**Publishes**

- `RiderOfferCreated`
- `RiderOfferExpired`
- `RiderAssigned`
- `NoRiderFound`

**Listens to**

- `UserRegistered` — create profile when role is RIDER
- `UserStatusChanged` — block suspended riders
- `OrderAccepted` — start matching
- `OrderDelivered` — free the rider
- `OrderCancelled` — stop matching or free the rider

## Calls other services

- Location: `GET /internal/riders/nearby`
- Restaurant: `GET /internal/restaurants/{id}` (pickup location)
