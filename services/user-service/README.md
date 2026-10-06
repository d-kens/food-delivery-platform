# User Service

Owns everything about a user: their login account and tokens, and their personal details and delivery addresses. It is the only service that can create tokens; all other services verify them with its public key.

**Gateway prefixes:** `/api/auth` (registration, login, sessions), `/api/users` (the user's details, addresses, admin user management)
