# API Gateway

The single public entry point for the frontend and simulator. It routes requests to the right service and protects the platform from overload. It does **not** authenticate or authorize; every service does that itself.

**Gateway prefix:** none (it owns all public routes)

## Responsibilities

- Route requests to services by path prefix
- Rate-limit by client IP (Redis)
- Handle CORS for the Angular app
- Proxy WebSocket connections to the Location Service
- Add a correlation ID to every request
- Forward the `Authorization` header unchanged

## Data it owns

No business data. Rate-limit counters in Redis.

## Endpoints

| Method | Path | Access | Description |
|---|---|---|---|
| `GET` | `/actuator/health` | Internal | Health check for Kubernetes probes |

Paths starting with `/internal` are for other services only. The gateway does not route them.

## Events

**Publishes**

- None

**Listens to**

- None

## Calls other services

- None

## Notes

### Routes

| Path | Routed to |
|---|---|
| `/api/auth/**` | auth-service |
| `/api/customers/**` | customer-service |
| `/api/restaurants/**` | restaurant-service |
| `/api/cart/**`, `/api/orders/**` | order-service |
| `/api/payments/**` | payment-service |
| `/api/dispatch/**` | dispatch-service |
| `/api/location/**`, `/ws` | location-service |
| `/api/notifications/**` | notification-service |
| `/api/search/**` | search-service |

`/internal/**` paths are never routed.
