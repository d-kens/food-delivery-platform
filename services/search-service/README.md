# Search Service

Full-text search over restaurants and dishes with typo tolerance and filters. Its index is a read-only copy of Restaurant Service data and can be rebuilt at any time.

**Gateway prefix:** `/api/search`

## Responsibilities

- Keep an Elasticsearch index of restaurants and menu items up to date from events
- Search with typo tolerance, filters and distance sorting
- Offer autocomplete suggestions
- Rebuild the index from scratch on demand

## Data it owns

Elasticsearch indices: `restaurants`, `menu_items`.

## Endpoints

| Method | Path | Access | Description |
|---|---|---|---|
| `GET` | `/api/search` | Public | Search: `q`, `cuisine`, `maxPrice`, `minRating`, `openNow`, `lat`, `lng` |
| `GET` | `/api/search/suggest` | Public | Autocomplete suggestions for `q` |
| `POST` | `/api/search/admin/reindex` | ADMIN | Rebuild the index from Restaurant Service |

Paths starting with `/internal` are for other services only. The gateway does not route them.

## Events

**Publishes**

- None

**Listens to**

- `RestaurantUpdated` — update the restaurant document
- `MenuUpdated` — update menu item documents

## Calls other services

- Restaurant: `GET /internal/restaurants/{id}` (during reindex)
