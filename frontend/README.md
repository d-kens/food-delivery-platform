# Frontend (Angular)

One Angular app with four lazy-loaded areas, each protected by a role guard:

| Area | Route | Users |
|---|---|---|
| Customer | `/customer` | Browse, search, cart, checkout, live tracking, order history |
| Restaurant | `/restaurant` | Incoming orders dashboard, menu and hours management, earnings |
| Rider | `/rider` | Go online, job offers, active delivery, earnings (installable PWA) |
| Admin | `/admin` | Approve accounts, manage orders, payouts, templates |

## Key points

- **One login page** (`/login`) for everyone. After login the app reads the role from the token and redirects to the right area.
- **Separate registration pages:** `/register`, `/register/restaurant`, `/register/rider`.
- **Talks only to the gateway:** REST at `/api/**`, WebSocket at `/ws`.
- **Tokens:** access token kept in memory; refresh token in an HttpOnly cookie. An HTTP interceptor adds the Bearer header and refreshes once on a 401.
- **Real-time:** one STOMP connection per user (`@stomp/rx-stomp`).
- **Maps:** Leaflet with OpenStreetMap.
- **Rider PWA:** `@angular/pwa`, browser Geolocation API, Web Push for job offers, Screen Wake Lock to keep the GPS active.
- **Deployed** as static files in an Nginx container.
