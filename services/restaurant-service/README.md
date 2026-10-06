# Restaurant Service

Owns restaurants, menus, prices and opening hours, and answers "restaurants near me" using PostGIS.

**Gateway prefix:** `/api/restaurants`

## Responsibilities

- Create a restaurant profile when a restaurant registers
- Manage menus, categories, items, prices and availability
- Manage opening hours
- Find open restaurants within a radius of a location
- Validate cart items and prices for the Order Service
- Hide restaurants that are pending or suspended

## Data it owns

`restaurant_db`: `restaurants` (owner user ID, name, cuisine, location point, status), `menu_categories`, `menu_items` (price, available), `opening_hours`, `outbox`.

## Endpoints

| Method | Path | Access | Description |
|---|---|---|---|
| `GET` | `/api/restaurants` | Public | Nearby restaurants: `lat`, `lng`, `radiusKm`, `cuisine`, `openNow` |
| `GET` | `/api/restaurants/{id}` | Public | Restaurant details |
| `GET` | `/api/restaurants/{id}/menu` | Public | Full menu with available items |
| `GET` | `/api/restaurants/me` | RESTAURANT | My restaurant profile |
| `PUT` | `/api/restaurants/me` | RESTAURANT | Update my restaurant profile and location |
| `PUT` | `/api/restaurants/me/hours` | RESTAURANT | Set opening hours |
| `GET` | `/api/restaurants/me/menu` | RESTAURANT | My menu including unavailable items |
| `POST` | `/api/restaurants/me/menu/categories` | RESTAURANT | Add a menu category |
| `POST` | `/api/restaurants/me/menu/items` | RESTAURANT | Add a menu item |
| `PUT` | `/api/restaurants/me/menu/items/{id}` | RESTAURANT (owner) | Update a menu item |
| `PATCH` | `/api/restaurants/me/menu/items/{id}/availability` | RESTAURANT (owner) | Mark an item available or sold out |
| `DELETE` | `/api/restaurants/me/menu/items/{id}` | RESTAURANT (owner) | Remove a menu item |
| `GET` | `/api/restaurants/admin` | ADMIN | List restaurants, filter by status |
| `GET` | `/internal/restaurants/{id}` | Internal | Restaurant details including location |
| `POST` | `/internal/menu/validate` | Internal | Validate item IDs and return current prices for checkout |

Paths starting with `/internal` are for other services only. The gateway does not route them.

## Events

**Publishes**

- `RestaurantUpdated`
- `MenuUpdated`

**Listens to**

- `UserRegistered` — create profile when role is RESTAURANT
- `UserStatusChanged` — show or hide the restaurant

## Calls other services

- None

## Notes

Accepting and preparing orders is done through the Order Service, which owns order state.
