# Payment Service

Handles money: charges, refunds, delivery fees, a double-entry ledger, and payouts. Uses a simulated payment provider that fails randomly on purpose.

**Gateway prefix:** `/api/payments`

## Responsibilities

- Calculate delivery fees from distance and demand (surge)
- Charge orders using the order ID as an idempotency key
- Refund cancelled orders
- Record every money movement in a double-entry ledger
- Record restaurant and rider earnings when an order is delivered
- Run a nightly Spring Batch job to calculate payouts
- Wrap the fake provider with Resilience4j (circuit breaker, retry, timeout)

## Data it owns

`payment_db`: `payments`, `refunds`, `ledger_entries`, `payouts`, `processed_events`, `outbox`.

## Endpoints

| Method | Path | Access | Description |
|---|---|---|---|
| `POST` | `/api/payments/quotes` | CUSTOMER | Preview delivery fee for a cart and address |
| `GET` | `/api/payments/orders/{orderId}` | CUSTOMER (owner), ADMIN | Payment and refund status for an order |
| `GET` | `/api/payments/me/earnings` | RESTAURANT, RIDER | My earnings, filter by date range |
| `GET` | `/api/payments/me/payouts` | RESTAURANT, RIDER | My payouts |
| `GET` | `/api/payments/admin/payouts` | ADMIN | All payouts, filter by date and status |
| `GET` | `/api/payments/admin/ledger` | ADMIN | Ledger entries, filter by account or order |
| `POST` | `/internal/quotes` | Internal | Fee calculation used by Order at checkout |

Paths starting with `/internal` are for other services only. The gateway does not route them.

## Events

**Publishes**

- `PaymentSucceeded`
- `PaymentFailed`
- `RefundCompleted`
- `PayoutCalculated`

**Listens to**

- `OrderCreated` — charge the customer
- `OrderCancelled` — refund if charged
- `OrderDelivered` — record restaurant and rider earnings

## Calls other services

- Fake payment provider (in-process module)
