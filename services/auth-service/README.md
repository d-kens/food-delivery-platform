# Auth Service

Owns login accounts and tokens. It is the only service that can create tokens; all other services verify them with its public key.

**Gateway prefix:** `/api/auth`

## Responsibilities

- Register customers and riders
- Log users in and hash passwords with BCrypt
- Issue RS256-signed access tokens (about 15 min) with claims `sub`, `role`, `iss`, `aud`, `exp`
- Issue, rotate and revoke refresh tokens (about 7 days, stored hashed); reuse of an old token revokes the whole token family
- Manage account roles: `CUSTOMER`, `RIDER`, `ADMIN` (plus `SERVICE` for service tokens)
- Manage account status: `PENDING`, `ACTIVE`, `SUSPENDED`
- Publish the public signing key (JWKS)
- Seed the first admin account at startup
- Issue service tokens for background service-to-service calls

## Data it owns

`auth_db`: `users` (email, password hash, role, status), `refresh_tokens` (hash, user, family ID, expiry, revoked), `signing_keys`, `outbox`.

## Endpoints

| Method | Path | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/register/customer` | Public | Create a customer account (active immediately). Restaurant owners and staff sign up here too |
| `POST` | `/api/auth/register/rider` | Public | Apply as a rider (pending approval) |
| `POST` | `/api/auth/login` | Public | Log in; returns access token and sets refresh-token cookie |
| `POST` | `/api/auth/refresh` | Public (cookie) | Rotate refresh token and return a new access token |
| `POST` | `/api/auth/logout` | Authenticated | Revoke refresh token and clear cookie |
| `GET` | `/api/auth/{id}` | Authenticated | Current account: email, role, status |
| `PUT` | `/api/auth/{id}/password` | Authenticated | Change password (revokes other sessions) |
| `GET` | `/api/auth/admin/users` | ADMIN | List accounts, filter by `role` and `status` |
| `PATCH` | `/api/auth/admin/users/{id}/status` | ADMIN | Approve, suspend or reactivate an account |
| `POST` | `/api/auth/admin/users` | ADMIN | Create another admin account |
| `GET` | `/.well-known/jwks.json` | Internal | Public signing keys used by all services to verify tokens |
| `POST` | `/internal/service-token` | Internal | Issue a short-lived token with role `SERVICE` |
| `GET` | `/internal/users?email={email}` | Internal | Look up a user ID by email (used to add restaurant members) |

Paths starting with `/internal` are for other services only. The gateway does not route them.

## Events

**Publishes**

- `UserRegistered`
- `UserStatusChanged`

**Listens to**

- None

## Calls other services

- None

## Notes

Registration publishes `UserRegistered` with the role and the extra details from the form, so the owning service can create the profile.

There is no restaurant role. Auth only knows who a user is; which restaurants they can manage, and as what, is owned by the Restaurant Service (see its `restaurant_members` table). Account status here is for the person's login only; restaurant approval lives in the Restaurant Service.
