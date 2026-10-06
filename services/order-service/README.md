# Order Service

Owns carts and orders, enforces the order state machine, and orchestrates the order saga: it reacts to payment and rider events and decides the next step, including cancellations.

**Gateway prefix:** `/api/cart, /api/orders`

## Responsibilities

- Manage each customer's cart (one restaurant per cart)
- Check out: validate items with Restaurant, snapshot the address from Customer, get a fee from Payment
- Store a snapshot of items, prices, address and fee on the order
- Enforce state transitions: `CREATED → PLACED → ACCEPTED → PREPARING → READY → PICKED_UP → DELIVERED`, plus `PAYMENT_FAILED` and `CANCELLED`
- Record every status change in a history table
- Run timeouts: auto-reject orders not answered by the restaurant within 10 minutes
- Let customers rate delivered orders

## Data it owns

`order_db`: `carts`, `cart_items`, `orders`, `order_items`, `order_status_history`, `ratings`, `idempotency_keys`, `processed_events`, `outbox`.

## Endpoints

| Method | Path | Access | Description |
|---|---|---|---|
| `GET` | `/api/cart` | CUSTOMER | Get my cart |
| `POST` | `/api/cart/items` | CUSTOMER | Add an item |
| `PUT` | `/api/cart/items/{itemId}` | CUSTOMER | Change quantity |
| `DELETE` | `/api/cart/items/{itemId}` | CUSTOMER | Remove an item |
| `DELETE` | `/api/cart` | CUSTOMER | Empty the cart |
| `POST` | `/api/orders` | CUSTOMER | Check out the cart (requires `Idempotency-Key` header) |
| `GET` | `/api/orders/me` | CUSTOMER | My order history |
| `GET` | `/api/orders/{id}` | Owner customer, restaurant or assigned rider; ADMIN | Order details and status history |
| `POST` | `/api/orders/{id}/cancel` | CUSTOMER (owner) | Cancel while still `PLACED` |
| `POST` | `/api/orders/{id}/rating` | CUSTOMER (owner) | Rate a delivered order |
| `GET` | `/api/orders/restaurant` | RESTAURANT | Orders for my restaurant, filter by status |
| `POST` | `/api/orders/{id}/accept` | RESTAURANT (owner) | Accept an order |
| `POST` | `/api/orders/{id}/reject` | RESTAURANT (owner) | Reject an order with a reason |
| `POST` | `/api/orders/{id}/preparing` | RESTAURANT (owner) | Mark as preparing |
| `POST` | `/api/orders/{id}/ready` | RESTAURANT (owner) | Mark as ready for pickup |
| `POST` | `/api/orders/{id}/picked-up` | RIDER (assigned) | Confirm pickup |
| `POST` | `/api/orders/{id}/delivered` | RIDER (assigned) | Confirm delivery |
| `GET` | `/api/orders/admin` | ADMIN | Search all orders |
| `POST` | `/api/orders/{id}/admin-cancel` | ADMIN | Cancel any order (triggers refund) |
| `GET` | `/internal/orders/{id}` | Internal | Order details for other services |

Paths starting with `/internal` are for other services only. The gateway does not route them.

## Events

**Publishes**

- `OrderCreated`
- `OrderPlaced`
- `OrderAccepted`
- `OrderRejected`
- `OrderPreparing`
- `OrderReady`
- `OrderPickedUp`
- `OrderDelivered`
- `OrderCancelled`

**Listens to**

- `PaymentSucceeded` — move to PLACED
- `PaymentFailed` — move to PAYMENT_FAILED
- `RiderAssigned` — save riderId on the order
- `NoRiderFound` — cancel the order
- `RefundCompleted` — mark the cancelled order as refunded

## Calls other services

- Restaurant: `POST /internal/menu/validate`
- Customer: `GET /internal/addresses/{id}`
- Payment: `POST /internal/quotes`
