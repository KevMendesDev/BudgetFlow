# Frontend

## Environment model

This frontend uses runtime configuration for the API base URL.

- `src/environments/environment.ts`: development fallback values used at build time.
- `src/environments/environment.prod.ts`: production fallback values used at build time.
- `public/app-config.json`: runtime configuration loaded by the browser before Angular bootstraps.

`apiBaseUrl` is resolved in this order:

1. `public/app-config.json`
2. `src/environments/environment*.ts`

## Development

During local development, `apiBaseUrl` can stay empty:

```json
{
  "apiBaseUrl": ""
}
```

In that case, requests go to `/api/...` and Angular's dev proxy forwards them to the backend.

## Production

Before serving the frontend, provide a real `public/app-config.json` value, for example:

```json
{
  "apiBaseUrl": "https://api.budgetflow.com"
}
```

This allows using the same frontend build artifact across environments without hardcoding the API URL in the bundle.
