# Employee Management & HR Portal

A realistic, production-shaped 3-tier web application — **React frontend →
Spring Boot backend → MySQL database** — built as a DevOps/Kubernetes
portfolio project. Designed from day one to be containerized with Docker
and later deployed on Kubernetes/K3s.

---

## 1. Project Structure

```
employee-management-system/
│
├── frontend/                  React (Vite) SPA
│   ├── src/
│   │   ├── api/                Axios client + API modules
│   │   ├── components/         Shared UI components
│   │   ├── context/             AuthContext (login state, roles)
│   │   ├── pages/               Login, Dashboard, Employees, etc.
│   │   └── styles/              Global CSS design system
│   ├── package.json
│   ├── Dockerfile               Multi-stage: React build → Nginx
│   └── nginx.conf
│
├── backend/                   Spring Boot REST API
│   ├── src/main/java/com/company/ems/
│   │   ├── controller/          REST endpoints
│   │   ├── service/              Business logic
│   │   ├── repository/           Spring Data JPA repositories
│   │   ├── entity/                JPA entities
│   │   ├── dto/                    Request/response objects
│   │   ├── mapper/                 Entity <-> DTO mapping
│   │   ├── security/                JWT filter, util, UserDetails
│   │   ├── config/                   Security, CORS, Swagger, DataSeeder
│   │   └── exception/                  Global exception handling
│   ├── src/test/java/…           Unit/integration tests
│   ├── pom.xml
│   └── Dockerfile
│
├── database/
│   └── init/                  Notes on schema/seed strategy
│
├── docker-compose.yml         Local 3-container stack
├── README.md                  This file
└── .gitignore
```

---

## 2. Required Software Versions

| Tool           | Version        |
|----------------|-----------------|
| Java (JDK)     | 17+             |
| Maven          | 3.9+            |
| Node.js        | 20+             |
| npm            | 10+             |
| MySQL          | 8.0             |
| Docker         | 24+             |
| Docker Compose | v2 (`docker compose`) |

---

## 3. Local Setup (without Docker)

### 3.1 Database

Install MySQL 8 locally, then create the database and user:

```sql
CREATE DATABASE ems_db;
CREATE USER 'ems_user'@'%' IDENTIFIED BY 'changeme';
GRANT ALL PRIVILEGES ON ems_db.* TO 'ems_user'@'%';
FLUSH PRIVILEGES;
```

The backend will create all tables automatically on first startup — you
don't need to run any schema SQL by hand.

### 3.2 Backend

```bash
cd backend

# Required environment variables (see full list in section 5)
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=ems_db
export DB_USERNAME=ems_user
export DB_PASSWORD=changeme
export JWT_SECRET=a-long-random-secret-at-least-32-characters
export CORS_ALLOWED_ORIGINS=http://localhost:5173

mvn clean install
mvn spring-boot:run
```

The API starts on **http://localhost:8080**. On first boot it seeds the
admin/HR users, departments, and sample employees automatically.

### 3.3 Frontend

```bash
cd frontend
cp .env.example .env     # defaults to http://localhost:8080/api
npm install
npm run dev
```

The app starts on **http://localhost:5173**.

---

## 4. Running with Docker Compose (recommended)

From the project root:

```bash
docker compose up --build
```

This builds and starts all three containers:

| Service  | Container    | Exposed Port |
|----------|--------------|--------------|
| mysql    | ems-mysql    | 3306         |
| backend  | ems-backend  | 8080         |
| frontend | ems-frontend | 3000         |

Then open **http://localhost:3000**.

Containers communicate over the internal `ems-network` Docker network
using **service names** (`mysql`, `backend`) — never `localhost` — exactly
as they will later communicate via Kubernetes Service DNS names.

To reset all data and start fresh:

```bash
docker compose down -v
docker compose up --build
```

---

## 5. Environment Variables

None of these are hardcoded anywhere in source code — all are read at
runtime (backend) or build time (frontend).

### Backend

| Variable                | Purpose                                   | Example / Default |
|--------------------------|---------------------------------------------|--------------------|
| `DB_HOST`                | MySQL host                                | `mysql` (Docker) / `localhost` |
| `DB_PORT`                | MySQL port                                | `3306` |
| `DB_NAME`                | Database name                             | `ems_db` |
| `DB_USERNAME`            | DB user                                   | `ems_user` |
| `DB_PASSWORD`            | DB password                               | *(set your own — never commit)* |
| `JWT_SECRET`              | HMAC signing key for JWTs, 32+ chars      | *(set your own — never commit)* |
| `JWT_EXPIRATION_MS`      | Token lifetime in ms                      | `86400000` (24h) |
| `CORS_ALLOWED_ORIGINS`   | Comma-separated allowed frontend origins  | `http://localhost:5173` |
| `DDL_AUTO`                | Hibernate schema strategy                 | `update` |
| `SERVER_PORT`             | Backend HTTP port                         | `8080` |

### Frontend

| Variable              | Purpose                          | Example |
|------------------------|-------------------------------------|---------|
| `VITE_API_BASE_URL`    | Base URL the SPA calls for the API | `http://localhost:8080/api` |

---

## 6. Default Login Credentials (demo data only)

These are seeded automatically by `DataSeeder` for local/demo use.
**Rotate or remove them before any real deployment.**

| Role     | Username | Password    |
|----------|----------|-------------|
| ADMIN    | `admin`  | `Admin@123` |
| HR_USER  | `hruser` | `Hr@12345`  |

---

## 7. API Endpoints

| Method | Endpoint                       | Access             | Description |
|--------|----------------------------------|----------------------|-------------|
| POST   | `/api/auth/login`               | Public              | Authenticate, receive JWT |
| GET    | `/api/employees`                | Authenticated       | List/search/filter/paginate employees |
| GET    | `/api/employees/{id}`           | Authenticated       | Get one employee |
| POST   | `/api/employees`                | ADMIN, HR_USER       | Create employee |
| PUT    | `/api/employees/{id}`           | ADMIN, HR_USER       | Update employee |
| DELETE | `/api/employees/{id}`           | ADMIN only           | Deactivate (soft-delete) employee |
| GET    | `/api/departments`               | Authenticated       | List departments |
| GET    | `/api/departments/{id}`         | Authenticated       | Get one department |
| POST   | `/api/departments`               | ADMIN only           | Create department |
| PUT    | `/api/departments/{id}`         | ADMIN only           | Update department |
| DELETE | `/api/departments/{id}`         | ADMIN only           | Delete department (must have 0 employees) |
| GET    | `/api/dashboard/statistics`      | Authenticated       | Aggregate dashboard stats |
| GET    | `/actuator/health`                | Public              | Health check (k8s-ready) |

Query params for `GET /api/employees`: `keyword`, `departmentId`, `status`
(`ACTIVE`/`INACTIVE`), `page`, `size`, `sortBy`, `sortDir`.

---

## 8. Swagger / API Documentation

Once the backend is running:

- Swagger UI: **http://localhost:8080/swagger-ui.html**
- OpenAPI JSON: **http://localhost:8080/v3/api-docs**

Click **Authorize** and paste `Bearer <your JWT>` (obtained from
`POST /api/auth/login`) to test protected endpoints directly in the UI.

---

## 9. Running Backend Tests

```bash
cd backend
mvn test
```

Tests cover: login (success/invalid credentials/validation), employee
creation, retrieval, update, deactivation, validation rules (email,
salary, duplicate email), and role-based access control (403 for
HR_USER attempting delete, 204 for ADMIN).

Tests run against an in-memory H2 database configured in
`src/test/resources/application.yml`, so they don't require MySQL.

---

## 10. Security Notes

- Passwords are hashed with **BCrypt** — never stored or logged in plain text.
- Authentication is **stateless JWT** — no server-side sessions.
- CORS is restricted to an **explicit allow-list** of origins (never `*`),
  since the API accepts an `Authorization` header.
- `DELETE /api/employees/{id}` and all `/api/departments` write operations
  are **ADMIN-only**, enforced with `@PreAuthorize` at the controller layer
  in addition to the URL-pattern rules in `SecurityConfig`.
- No secrets are committed to source control. In Kubernetes, `DB_PASSWORD`
  and `JWT_SECRET` should be provided via a **Kubernetes Secret** (mounted
  as env vars into the backend Deployment), while non-sensitive values like
  `DB_HOST` or `CORS_ALLOWED_ORIGINS` belong in a **ConfigMap**.

---

## 11. Kubernetes Readiness (for later)

This app was intentionally built so the following can be layered on
without code changes:

- **Frontend**: Deployment + Service + Ingress
- **Backend**: Deployment + Service + ConfigMap + Secret + HPA (Actuator's
  `/actuator/health/liveness` and `/actuator/health/readiness` are already
  exposed for probes)
- **Database**: StatefulSet + Service + PersistentVolumeClaim + Secret

Kubernetes manifests are **not** included yet — this project builds the
application layer first, deployment manifests come next.

---

## 12. Troubleshooting

**Backend won't start / can't connect to MySQL**
- Confirm MySQL is running and reachable at `DB_HOST:DB_PORT`.
- In Docker Compose, use the service name `mysql`, not `localhost`.
- Check `docker compose logs mysql` — the backend's `depends_on` health
  check waits for MySQL to be ready before starting.

**Frontend shows network errors / can't reach the API**
- Check `VITE_API_BASE_URL` in `frontend/.env` (local dev) or the
  `VITE_API_BASE_URL` build arg in `docker-compose.yml` (Docker). Vite
  env vars are baked in at **build time** — changing `.env` requires
  restarting `npm run dev` or rebuilding the Docker image.
- Check the backend's `CORS_ALLOWED_ORIGINS` includes the frontend's origin.

**401 Unauthorized on every request**
- Your JWT may have expired (default 24h) — log in again.
- Confirm the `Authorization: Bearer <token>` header is being sent (the
  Axios client in `src/api/axiosClient.js` does this automatically once
  you're logged in).

**403 Forbidden on delete/deactivate**
- Only `ADMIN` can deactivate employees or manage departments. Log in as
  `admin`, not `hruser`.

**"Cannot delete department" error**
- Departments can only be deleted when they have zero employees assigned.
  Reassign or deactivate those employees first.

**Port already in use**
- Change the host-side port mapping in `docker-compose.yml` (e.g.
  `"3001:80"` instead of `"3000:80"`), or stop the process using that port.

**`mvn` / `npm` not found**
- Install the required tooling from section 2, or use the Docker Compose
  path instead, which only requires Docker.
