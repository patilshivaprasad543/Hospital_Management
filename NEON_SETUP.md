# Neon PostgreSQL deployment

This project runs the Spring Boot application on Render and uses Neon PostgreSQL as its external database.

## Render environment variables

Set these on the Render web service. **Do not commit the password or a credential-bearing connection string to GitHub.**

- `SPRING_DATASOURCE_URL`: the Neon JDBC URL, for example:
  `jdbc:postgresql://<neon-host>/<database>?sslmode=require&channel_binding=require`
- `SPRING_DATASOURCE_USERNAME`: the Neon database username
- `SPRING_DATASOURCE_PASSWORD`: the Neon database password
- `DB_DIALECT`: `org.hibernate.dialect.PostgreSQLDialect`

The Neon pooled endpoint (hostname containing `-pooler`) can be used for the application. Neon documents pooled connections for reducing connection pressure and improving connection reuse. citeturn0search4

## Important: JDBC prefix

Neon displays a connection string beginning with `postgresql://` or `postgres://`. For this Spring Boot configuration, `SPRING_DATASOURCE_URL` must use the JDBC form:

`jdbc:postgresql://...`

Keep the query parameters supplied by Neon, including `sslmode=require` and `channel_binding=require`, when using the current Neon connection string format. citeturn0search1

## Local development

Without the Neon environment variables, the application falls back to the local PostgreSQL database configured in `application.properties`.

## Render

The repository's `render.yaml` intentionally leaves the three Neon credential variables as `sync: false`, so secrets are entered in the Render dashboard rather than stored in Git.

The health endpoint is:

`/health`

The GitHub Actions keep-alive workflow uses the Render health endpoint to reduce cold starts. It does not contain database credentials.

## Security

If a Neon password or SMTP app password has ever been committed to Git or shared publicly, rotate it before production deployment.
