# Food Delivery Platform

A microservices food delivery platform built as a learning project. Customers order from nearby restaurants, riders are matched to deliveries automatically, and customers watch their rider on a live map. Admins approve restaurants and riders and oversee the platform.

All data in this project is invented test data.

## Tech stack

- **Backend:** Java 21, Spring Boot, Spring Security (OAuth2 Resource Server), Spring Cloud Gateway
- **Messaging:** Kafka (outbox pattern, idempotent consumers)
- **Data:** PostgreSQL (one database per service, PostGIS for location queries), Redis, Elasticsearch
- **Frontend:** Angular (single app with Customer, Restaurant, Rider and Admin areas; rider area is a PWA), Leaflet maps
- **Real-time:** WebSocket with STOMP
- **Ops:** Docker Compose, Kubernetes, Helm, Terraform, GitHub Actions, Argo CD
- **Observability:** Actuator, Micrometer, Prometheus, Grafana, OpenTelemetry

## Repository layout

```
food-delivery-platform/
├── services/
│   ├── api-gateway/
│   ├── auth-service/
│   ├── customer-service/
│   ├── restaurant-service/
│   ├── order-service/
│   ├── payment-service/
│   ├── dispatch-service/
│   ├── location-service/
│   ├── notification-service/
│   └── search-service/
├── frontend/          Angular web app
├── simulator/         Fake riders and customers for load and testing
└── infra/             Docker Compose, Helm charts, Terraform, Argo CD
```

Each folder has its own README with details and the endpoints it exposes.

## Services at a glance

| Service | What it does | Gateway prefix |
|---|---|---|
| [API Gateway](services/api-gateway/README.md) | Single entry point. Routing, IP rate limiting, CORS, WebSocket proxy | — |
| [Auth](services/auth-service/README.md) | Accounts, registration, login, access and refresh tokens, account status | `/api/auth` |
| [Customer](services/customer-service/README.md) | Customer profiles and saved delivery addresses | `/api/customers` |
| [Restaurant](services/restaurant-service/README.md) | Restaurants, menus, opening hours, "restaurants near me" | `/api/restaurants` |
| [Order](services/order-service/README.md) | Carts, orders, order state machine, saga orchestration | `/api/cart`, `/api/orders` |
| [Payment](services/payment-service/README.md) | Charges, refunds, delivery fees, ledger, payouts | `/api/payments` |
| [Dispatch](services/dispatch-service/README.md) | Rider availability and matching riders to orders | `/api/dispatch` |
| [Location](services/location-service/README.md) | Live rider positions and the real-time WebSocket hub | `/api/location`, `/ws` |
| [Notification](services/notification-service/README.md) | Simulated SMS, email and web push | `/api/notifications` |
| [Search](services/search-service/README.md) | Full-text search over restaurants and dishes | `/api/search` |

## How the services work together

- **The gateway is the only public entry point.** The frontend and simulator never call services directly.
- **REST for questions, events for facts.** A service calls another over REST only when it needs an answer right now (for example, Order validating prices with Restaurant at checkout). Everything else is announced as a Kafka event (for example, `OrderPlaced`) and other services react.
- **Each service owns its data.** No service reads another service's database.
- **One WebSocket connection per user**, held by the Location Service, which pushes order updates, job offers and rider positions.

### Placing an order (summary)

1. Customer checks out → Order validates items (Restaurant), snapshots the address (Customer), gets a fee (Payment), saves the order as `CREATED` and publishes `OrderCreated`.
2. Payment charges the simulated provider → `PaymentSucceeded` → Order moves to `PLACED`.
3. Restaurant accepts → `OrderAccepted` → Dispatch finds the nearest rider and sends an offer.
4. Rider accepts → `RiderAssigned` → Location starts live tracking.
5. Restaurant marks `PREPARING` then `READY`; rider marks `PICKED_UP` then `DELIVERED`.
6. Payment records restaurant and rider earnings.

If payment fails the order becomes `PAYMENT_FAILED`. If the restaurant rejects, the customer cancels, or no rider is found within 15 minutes, the order becomes `CANCELLED` and Payment refunds it.

## Users and roles

| Role | Registration | Status after registering |
|---|---|---|
| `CUSTOMER` | Self sign-up | `ACTIVE` |
| `RESTAURANT` | Applies with business details | `PENDING` until an admin approves |
| `RIDER` | Applies with vehicle details | `PENDING` until an admin approves |
| `ADMIN` | No public sign-up; first admin is seeded, others created by an admin | `ACTIVE` |

Each account has exactly one role. Only `ACTIVE` accounts can log in.

## Security model

- **Auth Service** issues RS256-signed access tokens (about 15 minutes) and rotating refresh tokens (about 7 days, stored hashed, sent in an HttpOnly cookie). It publishes its public key at a JWKS endpoint.
- **The gateway does not authenticate or authorize.** It routes, applies IP rate limiting and CORS, and forwards the `Authorization` header unchanged.
- **Every service authenticates and authorizes.** Each service validates the JWT using Auth's public key, then checks:
  - **Role:** is the caller allowed to use this endpoint?
  - **Ownership:** is this the caller's own order, restaurant or delivery?
- **Service-to-service calls** forward the user's token, or use a service token from Auth for background work.

## Shared conventions

- **Base path:** all public endpoints are under `/api/<service>`. Internal endpoints are under `/internal/**` and are not exposed by the gateway.
- **Errors:** JSON in RFC 7807 Problem Details format.
- **Idempotency:** `POST /api/orders` requires an `Idempotency-Key` header.
- **Pagination:** `?page=0&size=20`, responses include total count.
- **Events:** every event has `eventId`, `eventType`, `occurredAt`, `version` and `payload`. Producers use the outbox pattern; consumers skip already-processed `eventId`s. Order events are keyed by `orderId`.
- **Tracing:** a correlation ID is added at the gateway and passed through calls and events.

## Build roadmap

1. **Foundations:** Gateway, Auth, Customer, Restaurant, frontend login and registration, Docker Compose.
2. **Ordering:** Order (state machine), Payment (fake provider), Kafka with outbox.
3. **Delivery:** Dispatch, Location hub, rider PWA, live map.
4. **Failure paths:** cancellations, refunds, timeouts, Resilience4j, dead-letter topics.
5. **Scale and insight:** Simulator, load tests, Notification, Search, observability.
6. **Production-like:** Terraform, Kubernetes, Helm, CI, Argo CD.
