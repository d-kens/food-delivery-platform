# Location Service (real-time hub)

Holds live rider positions and every WebSocket connection on the platform. Riders send their position here; customers, restaurants and riders receive live updates from here.

**Gateway prefix:** `/api/location, /ws`

## Responsibilities

- Accept WebSocket (STOMP) connections; validate the JWT on connect
- Receive rider positions every 3–5 seconds and store them in Redis (GEOADD)
- Answer "available riders near this point" for Dispatch (GEOSEARCH)
- Push the rider's position to the customer of the active order
- Push order status updates, new orders and job offers to the right users
- Check that only the assigned rider can send location for an order, and only its customer can subscribe

## Data it owns

Redis: rider positions, active tracking sessions (order → rider, customer). Optional `location_db` for sampled trip history.

## Endpoints

| Method | Path | Access | Description |
|---|---|---|---|
| `WS` | `/ws` | Authenticated | STOMP connection (token sent on connect) |
| `SEND` | `/app/location` | RIDER | Send my current position |
| `SUBSCRIBE` | `/user/queue/updates` | Authenticated | My personal updates: order status, new orders (restaurant), job offers (rider) |
| `SUBSCRIBE` | `/topic/orders/{orderId}/location` | CUSTOMER (owner) | Live rider position for my order |
| `GET` | `/api/location/orders/{orderId}/rider` | CUSTOMER (owner) | Last known rider position (for page reloads) |
| `GET` | `/internal/riders/nearby` | Internal | Nearest riders: `lat`, `lng`, `radiusKm`, `limit` |

Paths starting with `/internal` are for other services only. The gateway does not route them.

## Events

**Publishes**

- None

**Listens to**

- `RiderAssigned` — start a tracking session
- `OrderPickedUp / OrderDelivered / OrderCancelled` — update or end the session
- `Order status events` — push status to customer and restaurant
- `OrderPlaced` — push the new order to the restaurant
- `RiderOfferCreated / RiderOfferExpired` — push or withdraw the offer on the rider's app

## Calls other services

- None

## Notes

Positions are not published to Kafka; they are too frequent and only the latest one matters, so they stay in Redis.
