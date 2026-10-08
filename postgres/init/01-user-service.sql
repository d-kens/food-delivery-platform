SELECT 'CREATE USER "app-user-service" WITH PASSWORD ''secret'''
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'app-user-service')\gexec

SELECT 'CREATE DATABASE users OWNER "app-user-service"'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'users')\gexec
