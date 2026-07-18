# AGENTS.md

## Cursor Cloud specific instructions

BudgetFlow is a personal-finance app: an Angular 21 SPA in `frontend/` and a Spring Boot 4 / Java 21 REST API in `backend/`, backed by PostgreSQL 16. Standard commands are documented in `README.md`; only the non-obvious cloud caveats are captured here.

### Services and how to run them (start these each session; they are NOT in the update script)

The update script only refreshes dependencies. On a fresh VM you must start the infra + services yourself:

1. **PostgreSQL 16** (installed natively, not via Docker for the app): start with
   `sudo pg_ctlcluster 16 main start`. The `budgetflow` database and the `postgres`/`postgres` credentials already exist in the snapshot. Recreate if missing:
   `sudo -u postgres psql -c "ALTER USER postgres PASSWORD 'postgres';"` and
   `sudo -u postgres psql -c "CREATE DATABASE budgetflow;"`.
2. **Backend API** (`:8080`) from `backend/`. Do NOT use the default `dev` Docker-Compose Postgres; point Spring at the native DB and disable compose:
   ```bash
   cd backend
   SPRING_PROFILES_ACTIVE=dev SPRING_DOCKER_COMPOSE_ENABLED=false \
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/budgetflow \
   SPRING_DATASOURCE_USERNAME=postgres SPRING_DATASOURCE_PASSWORD=postgres \
   JWT_SECRET=dev-only-change-me-with-at-least-32-chars FRONTEND_URL=http://localhost:4200 \
   ./mvnw spring-boot:run
   ```
   Liquibase migrations run automatically on startup.
3. **Frontend dev server** (`:4200`) from `frontend/`: `npm start`. It proxies `/api`, `/oauth2`, `/login/oauth2` to `127.0.0.1:8080` (see `frontend/proxy.conf.json`).

### Critical gotcha: `frontend/public/app-config.json` points at production

The committed `frontend/public/app-config.json` has `"apiBaseUrl": "https://api.budgetflow.site"`. With that value the SPA calls the **production** API instead of the local backend (registration/login appear to fail locally with "Falha na requisição"). For local dev this file must be `{"apiBaseUrl": ""}` so the app uses the Angular proxy (as documented in `README.md`). Keep this as a local (uncommitted) change so the production frontend config is not altered.

### Email verification is required for password login, and Resend is not configured

Email/password login is blocked until `users.email_verified_at` is set, and no `RESEND_API_KEY` is present, so verification emails are never sent. To unblock a test account after registering, set it directly in the DB:
`UPDATE users SET email_verified_at = NOW() WHERE email='<email>';`
Passwords must contain uppercase, lowercase, a digit and a special character (e.g. `SenhaForte123!`). Google OAuth login needs real Google credentials and is optional.

### Tests

- Backend: `cd backend && ./mvnw test` — uses Testcontainers, so the Docker daemon must be running (`sudo dockerd &`). The `postgres:16` image is already pulled in the snapshot. The current user is in the `docker` group but the daemon must be started each session.
- Frontend: `cd frontend && npx ng test --watch=false` (Vitest). There is no dedicated lint step in the repo.
