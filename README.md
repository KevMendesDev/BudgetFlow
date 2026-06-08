# BudgetFlow

Aplicacao de controle e gerenciamento financeiro com frontend Angular e backend Spring Boot no mesmo repositorio.

## Estrutura

- `frontend/`: aplicacao web Angular
- `backend/`: API Spring Boot

## Pre-requisitos

- Node.js e npm
- Java 21
- Docker

## Como rodar localmente

### Backend

O backend usa o perfil `dev` para subir o PostgreSQL automaticamente via Docker Compose do Spring Boot.

```bash
cd backend
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Mais detalhes em [backend/README.md](/workspaces/BudgetFlow/backend/README.md).

### Frontend

Em outro terminal:

```bash
cd frontend
npm install
npm start
```

O frontend usa `public/app-config.json` como configuracao em runtime e, quando `apiBaseUrl` esta vazio, envia `/api/...` para o proxy de desenvolvimento.

Mais detalhes em [frontend/README.md](/workspaces/BudgetFlow/frontend/README.md).

## Configuracao

- Backend: use `SPRING_PROFILES_ACTIVE=dev` no ambiente local.
- Frontend: configure `public/app-config.json` para ambientes publicados.
- Segredos de producao nao devem ser salvos no repositorio.

## Testes

### Backend

```bash
cd backend
./mvnw test
```

### Frontend

```bash
cd frontend
npm test
```

## Proximos passos recomendados

- adicionar CI em `.github/workflows/`
- ampliar a cobertura de testes
- centralizar scripts de execucao na raiz do repositorio
