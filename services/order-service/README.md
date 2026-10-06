# Order Service

Owns carts and orders, enforces the order state machine, and orchestrates the order saga: it reacts to payment and rider events and decides the next step, including cancellations.

**Gateway prefix:** `/api/cart, /api/orders`

## Responsibilities

- Manage each customer's cart (one restaurant per cart)
- Quote the delivery fee for the cart and a chosen address (gathers both locations and asks Payment)
- Check out: validate items with Restaurant, snapshot the address from User, get a fee from Payment
- Store a snapshot of items, prices, address and fee on the order
- Enforce state transitions: `CREATED → PLACED → ACCEPTED → PREPARING → READY → PICKED_UP → DELIVERED`, plus `PAYMENT_FAILED` and `CANCELLED`. Every cancellation, whatever the cause, ends in `CANCELLED` with a reason: `CUSTOMER`, `ADMIN`, `REJECTED_BY_RESTAURANT`, `RESTAURANT_TIMEOUT` or `NO_RIDER_FOUND`
- Record every status change in a history table
- Run timeouts: cancel orders not answered by the restaurant within 10 minutes (reason `RESTAURANT_TIMEOUT`)
- Let customers rate delivered orders (restaurant score, optional rider score and comment) and publish `OrderRated`

## Data it owns

`order_db`: `carts`, `cart_items`, `orders`, `order_items`, `order_status_history`, `ratings`, `idempotency_keys`, `processed_events`, `outbox`.

## Events

**Publishes**

Every order event carries `orderId`, `customerId` and `restaurantId`. Extra payload where consumers need it:

- `OrderCreated` — total amount (Payment charges it)
- `OrderPlaced`
- `OrderAccepted` — pickup coordinates (restaurant) and drop-off coordinates (address snapshot), so Dispatch can match without calling other services
- `OrderPreparing`
- `OrderReady`
- `OrderPickedUp` — rider ID
- `OrderDelivered` — rider ID, food subtotal, delivery fee (Payment records earnings)
- `OrderCancelled` — reason (see above) and rider ID if one was assigned; Payment refunds if charged
- `OrderRated` — rider ID, restaurant score, rider score

**Listens to**

- `PaymentSucceeded` — move to PLACED
- `PaymentFailed` — move to PAYMENT_FAILED
- `RiderAssigned` — save riderId on the order
- `NoRiderFound` — cancel the order (reason `NO_RIDER_FOUND`)
- `RefundCompleted` — mark the cancelled order as refunded

## Calls other services

- Restaurant: `POST /internal/menu/validate`
- Restaurant: `GET /internal/restaurants/{id}/members/{userId}` (check the caller can act for the order's restaurant)
- User: `GET /internal/addresses/{id}`
- Payment: `POST /internal/quotes` (at quote time and at checkout, with restaurant and address coordinates)
