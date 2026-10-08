# Postgres init scripts

`db-init` runs every `*.sql` file here on each `docker compose up`. Scripts only create what's missing, so they're safe to re-run.

## Add a database for a new service

1. Copy `01-user-service.sql` to `02-<service>.sql` and change the names.
2. In `docker-compose.yml`, give the service `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and:
   ```yaml
   depends_on:
     db-init:
       condition: service_completed_successfully
   ```
3. Run `docker compose up`.

## Change an existing user or database

Editing a script won't change what already exists. Run the change by hand or reset with `docker compose down -v` (deletes local data).
