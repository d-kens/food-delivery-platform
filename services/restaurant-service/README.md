# Restaurant Service

Owns restaurants, their members, menus, prices, opening hours and ratings. Answers "restaurants near me" using PostGIS and text search over restaurants and dishes using Postgres full-text search with `pg_trgm` for typo tolerance.

**Gateway prefix:** `/api/restaurants`

## Responsibilities

- Create a restaurant when a logged-in user asks for one, and make that user its `OWNER`
- Manage restaurant members and their roles (`OWNER`, `STAFF`)
- Manage restaurant status: `PENDING`, `ACTIVE`, `SUSPENDED` (set by an admin)
- Manage menus, categories, items, prices and availability
- Manage opening hours
- Find open restaurants within a radius of a location
- Search restaurants and dishes by text, with typo tolerance and filters
- Keep each restaurant's average rating up to date from `OrderRated` events
- Validate cart items and prices for the Order Service
- Tell other services whether a user is a member of a restaurant, and with which role
- Hide restaurants that are pending or suspended

## Data it owns

`restaurant_db`: `restaurants` (name, cuisine, location point, status, average rating, rating count, search vector), `restaurant_members` (restaurant ID, user ID, role), `menu_categories`, `menu_items` (price, available), `opening_hours`, `processed_events`, `outbox`.

## Restaurant creation and ownership

1. A person signs up for a normal account in the User Service.
2. They call `POST /api/restaurants` with the restaurant details.
3. The restaurant is created as `PENDING`, and the caller is added to `restaurant_members` as `OWNER`.
4. An admin reviews it and sets it to `ACTIVE`. Only then does it appear in search and accept orders.
5. The owner can add other existing users as `OWNER` or `STAFF`.

A user can be a member of several restaurants. Access to `/api/restaurants/{id}/...` is decided by the caller's membership in restaurant `{id}`, not by their account role.

| Restaurant role | Can do |
|---|---|
| `OWNER` | Everything: profile, hours, menu, members |
| `STAFF` | See the full menu, mark items available or sold out, handle orders |

A restaurant must always have at least one `OWNER`; removing the last one is rejected.

## Events

**Publishes**

- `RestaurantCreated` — includes the owner user ID
- `RestaurantStatusChanged` — includes the new status and the owner user IDs
- `RestaurantUpdated`
- `MenuUpdated`

**Listens to**

- `OrderRated` — update the restaurant's average rating

## Calls other services

- User: `GET /internal/users?email={email}` (find the user when adding a member)

## Notes

Accepting and preparing orders is done through the Order Service, which owns order state. It checks that the caller is a member of the order's restaurant with `GET /internal/restaurants/{id}/members/{userId}`.
