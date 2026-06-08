# Backend Configuration

## Configuration model

- `application.yml`: common non-sensitive defaults.
- `application-dev.yml`: local development profile. Starts PostgreSQL through Spring Docker Compose support.
- `application-prod.yml`: production profile. Requires all sensitive values from the environment.
- `.env.example`: reference only. Spring Boot does not load this file automatically.

## Local development

Requirements:
- Docker running
- `SPRING_PROFILES_ACTIVE=dev`

Start the backend:

```bash
cd backend
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

With the `dev` profile active, Spring Boot uses `src/main/docker/postgresql.yml` to start PostgreSQL automatically.

## Production

Set these environment variables in the runtime platform or container environment:

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS`
- `COOKIE_SECURE`
- `COOKIE_DOMAIN` when needed

Do not store production secrets in repository files.
