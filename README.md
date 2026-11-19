# Food Delivery Platform

## Local setup

Postgres, Kafka and the Tyk gateway run in Docker. The services and the frontend run on your machine.

1. Start the infrastructure:
   ```bash
   docker compose up -d
   ```
2. Start user-service with the `dev` profile (port 9001), from `services/user-service`:
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```
3. Start the frontend, from `frontend`:
   ```bash
   npm start
   ```
4. Open http://localhost:4200.

## How requests flow

```
browser → ng serve (/api) → Tyk :8080 → user-service :9001
```

- Every browser request goes through Tyk. Tyk copies the `access_token` cookie into the `Authorization: Bearer` header.
- Services only read the `Authorization` header, so calling a service directly needs a bearer token (e.g. from Postman).
- Each service has its own gateway prefix, e.g. `/api/user-service/`.
- After changing Tyk config, run `docker compose up -d tyk-gateway`.

## Ports

| What | Port |
| --- | --- |
| Frontend | 4200 |
| Tyk gateway | 8080 |
| user-service | 9001 |
| Postgres | 5433 |
| Kafka | 9092 |
| Redpanda Console | 8090 |
