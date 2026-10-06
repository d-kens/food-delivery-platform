# Payment Service

Handles money: charges, refunds, delivery fees, a double-entry ledger, and payouts. Uses a simulated payment provider that fails randomly on purpose.

**Gateway prefix:** `/api/payments`

## Responsibilities

- Calculate delivery fees from distance between restaurant and delivery address (Order supplies both coordinates)
- Charge orders using the order ID as an idempotency key
- Refund cancelled orders
- Record every money movement in a double-entry ledger
- Record restaurant and rider earnings when an order is delivered; restaurant earnings and payouts belong to the restaurant, not to a user
- Run a nightly Spring Batch job to calculate payouts
- Wrap the fake provider with Resilience4j (circuit breaker, retry, timeout)

## Data it owns

`payment_db`: `payments`, `refunds`, `ledger_entries`, `payouts`, `processed_events`, `outbox`.

## Events

**Publishes**

- `PaymentSucceeded`
- `PaymentFailed`
- `RefundCompleted`
- `PayoutCalculated`

**Listens to**

- `OrderCreated` — charge the customer the order total
- `OrderCancelled` — refund if charged (covers customer, admin, restaurant rejection, timeout and no-rider cancellations)
- `OrderDelivered` — record restaurant and rider earnings from the subtotal and fee in the event

## Calls other services

- Fake payment provider (in-process module)
- Restaurant: `GET /internal/restaurants/{id}/members/{userId}` (check the caller owns the restaurant)
