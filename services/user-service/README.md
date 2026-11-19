# User Service

Owns everything about a user: their login account and tokens, and their personal details and delivery addresses. It is the only service that can create tokens; all other services verify them with its public key.

**Gateway prefix:** `/api/user-service/`, covering `/auth` (registration, login, sessions, password reset) and `/users` (the user's details, addresses, admin user management)
