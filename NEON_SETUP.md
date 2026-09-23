# Neon PostgreSQL deployment

This project is configured to use Neon PostgreSQL as the external database while the Spring Boot application continues to run on Render.

## Render environment variables

Set these on the Render web service:

- `SPRING_DATASOURCE_URL`: the full Neon JDBC URL, for example `jdbc:postgresql://<host>/<database>?sslmode=require`
- `SPRING_DATASOURCE_USERNAME`: the Neon database username
- `SPRING_DATASOURCE_PASSWORD`: the Neon database password
- `DB_DIALECT`: `org.hibernate.dialect.PostgreSQLDialect`

Keep the Neon password and credential-bearing connection strings out of GitHub.

## Local development

Without the Neon environment variables, the application falls back to a local PostgreSQL database at `localhost:5432/hospital_db`.

The old Render-managed PostgreSQL resource is no longer defined in `render.yaml`.
