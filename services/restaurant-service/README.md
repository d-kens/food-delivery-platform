# Restaurant Service

Owns restaurants, their members, menus, prices and opening hours, and answers "restaurants near me" using PostGIS.

**Gateway prefix:** `/api/restaurants`

## Responsibilities

- Create a restaurant when a logged-in user asks for one, and make that user its `OWNER`
- Manage restaurant members and their roles (`OWNER`, `STAFF`)
- Manage restaurant status: `PENDING`, `ACTIVE`, `SUSPENDED` (set by an admin)
- Manage menus, categories, items, prices and availability
- Manage opening hours
- Find open restaurants within a radius of a location
- Validate cart items and prices for the Order Service
- Tell other services whether a user is a member of a restaurant, and with which role
- Hide restaurants that are pending or suspended

## Data it owns

`restaurant_db`: `restaurants` (name, cuisine, location point, status), `restaurant_members` (restaurant ID, user ID, role), `menu_categories`, `menu_items` (price, available), `opening_hours`, `outbox`.

## Restaurant creation and ownership

1. A person signs up for a normal account in the Auth Service.
2. They call `POST /api/restaurants` with the restaurant details.
3. The restaurant is created as `PENDING`, and the caller is added to `restaurant_members` as `OWNER`.
4. An admin reviews it and sets it to `ACTIVE`. Only then does it appear in search and accept orders.
5. The owner can add other existing users as `OWNER` or `STAFF`.

A user can be a member of several restaurants. Access to `/api/restaurants/{id}/...` is decided by the caller's membership in restaurant `{id}`, not by their Auth role.

| Restaurant role | Can do |
|---|---|
| `OWNER` | Everything: profile, hours, menu, members |
| `STAFF` | See the full menu, mark items available or sold out, handle orders |

A restaurant must always have at least one `OWNER`; removing the last one is rejected.

## Endpoints

| Method | Path | Access | Description |
|---|---|---|---|
| `GET` | `/api/restaurants` | Public | Nearby active restaurants: `lat`, `lng`, `radiusKm`, `cuisine`, `openNow` |
| `GET` | `/api/restaurants?memberId={id}` | Authenticated (self) | Restaurants the user is a member of, with their role and the restaurant status |
| `POST` | `/api/restaurants` | Authenticated | Create a restaurant (`PENDING`); the caller becomes `OWNER` |
| `GET` | `/api/restaurants/{id}` | Public; members | Restaurant details; members also get the full profile and status |
| `PUT` | `/api/restaurants/{id}` | OWNER | Update restaurant profile and location |
| `PUT` | `/api/restaurants/{id}/hours` | OWNER | Set opening hours |
| `GET` | `/api/restaurants/{id}/menu` | Public; members | Menu with available items; members also see unavailable items |
| `POST` | `/api/restaurants/{id}/menu/categories` | OWNER | Add a menu category |
| `POST` | `/api/restaurants/{id}/menu/items` | OWNER | Add a menu item |
| `PUT` | `/api/restaurants/{id}/menu/items/{itemId}` | OWNER | Update a menu item |
| `PATCH` | `/api/restaurants/{id}/menu/items/{itemId}/availability` | OWNER, STAFF | Mark an item available or sold out |
| `DELETE` | `/api/restaurants/{id}/menu/items/{itemId}` | OWNER | Remove a menu item |
| `GET` | `/api/restaurants/{id}/members` | OWNER | List members and their roles |
| `POST` | `/api/restaurants/{id}/members` | OWNER | Add an existing user by email with a role |
| `DELETE` | `/api/restaurants/{id}/members/{userId}` | OWNER | Remove a member (not the last owner) |
| `GET` | `/api/restaurants/admin` | ADMIN | List restaurants, filter by status |
| `PATCH` | `/api/restaurants/admin/{id}/status` | ADMIN | Approve, suspend or reactivate a restaurant |
| `GET` | `/internal/restaurants/{id}` | Internal | Restaurant details including location |
| `GET` | `/internal/restaurants/{id}/members/{userId}` | Internal | The user's role in the restaurant, or 404 if not a member |
| `POST` | `/internal/menu/validate` | Internal | Validate item IDs and return current prices for checkout |

Paths starting with `/internal` are for other services only. The gateway does not route them.

## Events

**Publishes**

- `RestaurantCreated` — includes the owner user ID
- `RestaurantStatusChanged` — includes the new status and the owner user IDs
- `RestaurantUpdated`
- `MenuUpdated`

**Listens to**

- None

## Calls other services

- Auth: `GET /internal/users?email={email}` (find the user when adding a member)

## Notes

Accepting and preparing orders is done through the Order Service, which owns order state. It checks that the caller is a member of the order's restaurant with `GET /internal/restaurants/{id}/members/{userId}`.
