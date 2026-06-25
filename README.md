# BudgetFlow

Aplicacao web para controle financeiro pessoal. O sistema permite organizar receitas e despesas por periodo, categoria e tipo de pagamento, alem de gerenciar transacoes recorrentes e acompanhar os lancamentos mensais.

## Funcionalidades

- Cadastro e login de usuarios, com suporte a OAuth2 Google.
- Controle de categorias financeiras.
- Controle de periodos financeiros.
- Cadastro de receitas e despesas.
- Cadastro e sincronizacao de transacoes recorrentes.
- Frontend Angular consumindo API Spring Boot.

## Stack

- Frontend: Angular 21, TypeScript, SCSS, Chart.js.
- Backend: Java 21, Spring Boot 4, Spring Security, JPA, Liquibase.
- Banco: PostgreSQL 16.
- Testes: JUnit/Testcontainers no backend e Vitest/Angular no frontend.

## Estrutura

```text
backend/   API REST Spring Boot
frontend/  Aplicacao Angular
deploy/    Arquivos auxiliares de deploy
```

## Pre-requisitos

- Java 21
- Node.js e npm
- Docker

## Configuracao

### Banco local

Suba o PostgreSQL usado em desenvolvimento:

```bash
cd backend
docker compose -f src/main/docker/postgresql.yml up -d
```

Dados padrao:

```text
database: budgetflow
user: postgres
password: postgres
host: localhost
port: 5432
```

### Variaveis do backend

O arquivo `backend/.env.example` serve como referencia. O Spring Boot nao carrega esse arquivo automaticamente.

Para desenvolvimento, configure pelo terminal, IDE ou run configuration:

```bash
SPRING_PROFILES_ACTIVE=dev
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/budgetflow
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
JWT_SECRET=dev-only-change-me-with-at-least-32-chars
FRONTEND_URL=http://localhost:4200
GOOGLE_CLIENT_ID=seu-client-id
GOOGLE_CLIENT_SECRET=seu-client-secret
```

Se usar o profile `prod`, tambem configure:

```bash
DB_URL=jdbc:postgresql://localhost:5432/budgetflow
DB_USERNAME=postgres
DB_PASSWORD=postgres
CORS_ALLOWED_ORIGINS=https://seu-front.com
COOKIE_SECURE=true
```

Para login com Google, cadastre no Google Cloud Console o redirect URI:

```text
http://localhost:8080/login/oauth2/code/google
```

### Configuracao do frontend

Em desenvolvimento, `frontend/proxy.conf.json` encaminha `/api`, `/oauth2` e `/login/oauth2` para `http://127.0.0.1:8080`.

O arquivo `frontend/public/app-config.json` controla a URL da API em runtime:

```json
{
  "apiBaseUrl": ""
}
```

Deixe vazio para usar o proxy local. Em ambiente publicado, informe a URL do backend.

## Como executar

### Backend

Linux/macOS:

```bash
cd backend
SPRING_PROFILES_ACTIVE=dev \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/budgetflow \
SPRING_DATASOURCE_USERNAME=postgres \
SPRING_DATASOURCE_PASSWORD=postgres \
JWT_SECRET=dev-only-change-me-with-at-least-32-chars \
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="dev"
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/budgetflow"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="postgres"
$env:JWT_SECRET="dev-only-change-me-with-at-least-32-chars"
.\mvnw.cmd spring-boot:run
```

A API sobe em:

```text
http://localhost:8080
```

### Frontend

```bash
cd frontend
npm install
npm start
```

A aplicacao sobe em:

```text
http://localhost:4200
```

## Comandos uteis

Backend:

```bash
cd backend
./mvnw test
./mvnw clean package
```

Frontend:

```bash
cd frontend
npm test
npm run build
```

## Observacoes

- As migrations do banco ficam em `backend/src/main/resources/db/changelog`.
- Segredos reais nao devem ser versionados.
- O profile `dev` possui valores padrao para desenvolvimento, mas o datasource ainda deve ser informado por variaveis de ambiente.
