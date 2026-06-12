import { loadRuntimeConfig } from './app/core/config/runtime-config';
import { applyInitialTheme } from './app/core/services/theme.service';

async function main(): Promise<void> {
  applyInitialTheme();
  await loadRuntimeConfig();

  const [{ bootstrapApplication }, { appConfig }, { App }] = await Promise.all([
    import('@angular/platform-browser'),
    import('./app/app.config'),
    import('./app/app'),
  ]);

  await bootstrapApplication(App, appConfig);
}

main().catch((err) => console.error(err));
