users
-------------------------
id              UUID PK
first_name
last_name
email           UNIQUE
phone           UNIQUE
password_hash
role            ENUM('USER', 'ADMIN')
status          ENUM('ACTIVE', 'SUSPENDED')
created_at
updated_at


docker compose exec user-service-db psql -U app-user-service -d users - This connects from inside the container, so you don't need a port.

\l - list databases
\dt - list tables
\d table_name - describe a table
\c dbname - switch database
