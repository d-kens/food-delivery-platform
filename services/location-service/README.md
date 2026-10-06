# Location Service (real-time hub)

Holds live rider positions and every WebSocket connection on the platform. Riders send their position here; customers, restaurants and riders receive live updates from here.

**Gateway prefix:** `/api/location, /ws`

## Responsibilities

- Accept WebSocket (STOMP) connections; validate the JWT on connect
- Receive rider positions every 3–5 seconds and store them in Redis (GEOADD); drop riders that have not sent a position for 30 seconds
- Answer "riders near this point" for Dispatch (GEOSEARCH); Dispatch filters by availability
- Push the rider's position to the customer of the active order
- Push order status updates and job offers to the right users, and new orders and status changes to the restaurant's channel
- Check that only the assigned rider can send location for an order, only its customer can subscribe to it, and only members of a restaurant can subscribe to its channel

## Data it owns

Redis: rider positions and last-seen times, active tracking sessions (order → rider, customer). Optional `location_db` for sampled trip history.

## Events

**Publishes**

- None

**Listens to**

- `RiderAssigned` — start a tracking session
- `OrderPickedUp / OrderDelivered / OrderCancelled` — update or end the session
- `Order status events` — push status to the customer, the restaurant's channel and the assigned rider
- `OrderPlaced` — push the new order to the restaurant's channel
- `RiderOfferCreated / RiderOfferExpired` — push or withdraw the offer on the rider's app

## Calls other services

- Restaurant: `GET /internal/restaurants/{id}/members/{userId}` (check membership when someone subscribes to a restaurant channel)

## Notes

Positions are not published to Kafka; they are too frequent and only the latest one matters, so they stay in Redis.
