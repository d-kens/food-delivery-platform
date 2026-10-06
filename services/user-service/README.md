# User Service

Owns everything about a user: their login account and tokens, and their personal details and delivery addresses. It is the only service that can create tokens; all other services verify them with its public key.

**Gateway prefixes:** `/api/auth` (registration, login, sessions), `/api/users` (the user's details, addresses, admin user management)

## Modules

The service has two packages with a strict boundary, so it can be split again later if needed:

| Package | Owns | Tables |
|---|---|---|
| `identity` | Accounts, passwords, roles, status, access and refresh tokens, signing keys, service tokens | `accounts`, `refresh_tokens`, `signing_keys` |
| `profile` | Name, phone, delivery addresses, contact lookups for other services | `profiles`, `addresses` |

`profile` never reads or writes `identity` tables directly; it goes through the `identity` package's public classes, and the other way round.

## Responsibilities

- Register customers and riders, creating the account and profile in one transaction
- Log users in and hash passwords with BCrypt
- Issue RS256-signed access tokens (about 15 min) with claims `sub`, `role`, `iss`, `aud`, `exp`
- Issue, rotate and revoke refresh tokens (about 7 days, stored hashed); reuse of an old token revokes the whole token family
- Manage account roles: `CUSTOMER`, `RIDER`, `ADMIN` (plus `SERVICE` for service tokens)
- Manage account status: `PENDING`, `ACTIVE`, `SUSPENDED`
- Publish the public signing key (JWKS)
- Seed the first admin account at startup
- Issue service tokens for background service-to-service calls
- Let users view and update their name and phone
- Manage customers' saved delivery addresses with coordinates
- Provide address snapshots to the Order Service at checkout
- Provide contact details (name, email, phone) and email lookups to other services

## Data it owns

`user_db`: `accounts` (user ID, email, password hash, role, status), `refresh_tokens` (hash, user, family ID, expiry, revoked), `signing_keys`, `profiles` (user ID, name, phone), `addresses` (user ID, label, street, city, latitude, longitude, default flag), `outbox`.

## Endpoints

| Method | Path | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/register/customer` | Public | Create a customer account (active immediately). Restaurant owners and staff sign up here too |
| `POST` | `/api/auth/register/rider` | Public | Apply as a rider (pending approval) |
| `POST` | `/api/auth/login` | Public | Log in; returns access token and sets refresh-token cookie |
| `POST` | `/api/auth/refresh` | Public (cookie) | Rotate refresh token and return a new access token |
| `POST` | `/api/auth/logout` | Authenticated | Revoke refresh token and clear cookie |
| `PUT` | `/api/auth/me/password` | Authenticated | Change my password (revokes other sessions) |
| `GET` | `/api/users/me` | Authenticated | My details: email, role, status, name, phone |
| `PUT` | `/api/users/me` | Authenticated | Update my name and phone |
| `GET` | `/api/users/me/addresses` | CUSTOMER | List my addresses |
| `POST` | `/api/users/me/addresses` | CUSTOMER | Add an address |
| `PUT` | `/api/users/me/addresses/{addressId}` | CUSTOMER (owner) | Update an address |
| `DELETE` | `/api/users/me/addresses/{addressId}` | CUSTOMER (owner) | Delete an address |
| `GET` | `/api/users/admin` | ADMIN | List users, filter by `role` and `status` |
| `GET` | `/api/users/admin/{id}` | ADMIN | View a user |
| `PATCH` | `/api/users/admin/{id}/status` | ADMIN | Approve, suspend or reactivate an account |
| `POST` | `/api/users/admin` | ADMIN | Create another admin account |
| `GET` | `/.well-known/jwks.json` | Internal | Public signing keys used by all services to verify tokens |
| `POST` | `/internal/service-token` | Internal | Issue a short-lived token with role `SERVICE` |
| `GET` | `/internal/users/{id}` | Internal | Contact details (name, email, phone) for other services |
| `GET` | `/internal/users?email={email}` | Internal | Look up a user ID by email (used to add restaurant members) |
| `GET` | `/internal/addresses/{id}` | Internal | Address snapshot for checkout (checks it belongs to the customer) |

Paths starting with `/internal` are for other services only. The gateway does not route them.

## Events

**Publishes**

- `UserRegistered` — after the account exists; for side effects only (welcome or "application received" message)
- `UserStatusChanged` — approval, suspension, reactivation

**Listens to**

- None

## Calls other services

- Dispatch: `PUT /internal/riders/{userId}` (create the rider record when registering a rider)

## Notes

### Registration flow

The same flow is used for customers, riders and admins (including the seeded admin).

1. Validate the form and check the email is not taken.
2. Generate the user ID.
3. For riders only: `PUT /internal/riders/{userId}` on Dispatch with vehicle details.
4. In one transaction: save the account, the profile and `UserRegistered` in the outbox.
5. Return `201`.

If the Dispatch call fails, nothing is saved and the user gets an error and can retry. The call is idempotent (keyed by user ID), so a retry never creates a duplicate rider record. A rider record left in Dispatch without an account is harmless: nobody can log in as it.

### Restaurants

There is no restaurant role. This service only knows who a user is; which restaurants they can manage, and as what, is owned by the Restaurant Service (see its `restaurant_members` table). Account status here is for the person's login only; restaurant approval lives in the Restaurant Service.
