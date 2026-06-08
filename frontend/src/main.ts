import { loadRuntimeConfig } from './app/core/config/runtime-config';

async function main(): Promise<void> {
  await loadRuntimeConfig();

  const [{ bootstrapApplication }, { appConfig }, { App }] = await Promise.all([
    import('@angular/platform-browser'),
    import('./app/app.config'),
    import('./app/app'),
  ]);

  await bootstrapApplication(App, appConfig);
}

main().catch((err) => console.error(err));
