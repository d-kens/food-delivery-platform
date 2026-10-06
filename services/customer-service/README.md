# Customer Service

Owns customer profiles and saved delivery addresses.

**Gateway prefix:** `/api/customers`

## Responsibilities

- Create a customer profile when a customer registers
- Let customers update their profile
- Manage saved delivery addresses with coordinates
- Provide address snapshots to the Order Service at checkout

## Data it owns

`customer_db`: `customers` (user ID, name, phone), `addresses` (label, street, city, latitude, longitude, default flag).

## Endpoints

| Method | Path | Access | Description |
|---|---|---|---|
| `GET` | `/api/customers/me` | CUSTOMER | Get my profile |
| `PUT` | `/api/customers/me` | CUSTOMER | Update my profile |
| `GET` | `/api/customers/me/addresses` | CUSTOMER | List my addresses |
| `POST` | `/api/customers/me/addresses` | CUSTOMER | Add an address |
| `PUT` | `/api/customers/me/addresses/{id}` | CUSTOMER (owner) | Update an address |
| `DELETE` | `/api/customers/me/addresses/{id}` | CUSTOMER (owner) | Delete an address |
| `GET` | `/api/customers/admin` | ADMIN | List customers |
| `GET` | `/api/customers/admin/{id}` | ADMIN | View a customer |
| `GET` | `/internal/customers/{id}` | Internal | Customer details for other services |
| `GET` | `/internal/addresses/{id}` | Internal | Address snapshot for checkout (checks it belongs to the customer) |

Paths starting with `/internal` are for other services only. The gateway does not route them.

## Events

**Publishes**

- `CustomerProfileUpdated`

**Listens to**

- `UserRegistered` — create profile when role is CUSTOMER

## Calls other services

- None
