# Neon PostgreSQL deployment

This project is configured to use Neon PostgreSQL as the external database while the Spring Boot application can continue to run on Render.

## Render environment variables

In the Render web service, set:

- `SPRING_DATASOURCE_URL`: the JDBC PostgreSQL connection string from Neon, using the form `jdbc:postgresql://<host>/<database>?sslmode=require`
- `SPRING_DATASOURCE_USERNAME`: the Neon database username
- `SPRING_DATASOURCE_PASSWORD`: the Neon database password
- `DB_DIALECT`: `org.hibernate.dialect.PostgreSQLDialect`

Do not commit the Neon password or connection string containing credentials to GitHub.

## Neon Free plan note

Neon's current Free plan provides PostgreSQL storage and compute allowances for development/small projects, but its compute scales to zero after inactivity. The application may therefore experience a short wake-up delay after the database has been idle.

## Local Eclipse development

If the Neon variables are not present, `application.properties` falls back to a local PostgreSQL database at `localhost:5432/hospital_db`.

For local Neon testing, set the same three `SPRING_DATASOURCE_*` variables in the IDE run configuration.

## Migration

The old Render PostgreSQL database definition was removed from `render.yaml`. Existing Render web-service deployment is retained; only the database provider is externalized to Neon.
