# Database Initialization

This folder is mounted into the MySQL container at
`/docker-entrypoint-initdb.d`, so any `.sql` file placed here runs
automatically the **first** time the `mysql` container starts with an
empty data volume.

## Current strategy

For this project, table creation and demo data are handled by the
**backend application itself**, not by scripts in this folder:

- **Schema**: Spring Boot / Hibernate creates the `users`, `departments`
  and `employees` tables automatically on startup via
  `spring.jpa.hibernate.ddl-auto=update` (see `backend/src/main/resources/application.yml`).
- **Seed data**: `com.company.ems.config.DataSeeder` inserts the admin
  and HR demo users, five departments, and twelve sample employees the
  first time the application boots against an empty database.

This keeps schema and seed data version-controlled in Java, in one
place, rather than duplicated between SQL scripts and JPA entities —
and it's what actually seeds the demo data you'll use to test the app.

## Where you'd add real init scripts

In a production-grade pipeline you would typically replace
`ddl-auto=update` with versioned migrations (e.g. **Flyway** or
**Liquibase**) and might use this folder only for one-time, DBA-owned
setup such as creating additional database users or extensions. If you
add such scripts later, name them with a numeric prefix so they run in
order, e.g.:

```
01-create-additional-user.sql
02-grants.sql
```

## Manually resetting the database

To start completely fresh (re-run the seeder, re-create the schema):

```bash
docker compose down -v   # removes the ems_mysql_data volume
docker compose up --build
```
