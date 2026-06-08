export interface RuntimeConfig {
  apiBaseUrl?: string;
}

let runtimeConfig: RuntimeConfig = {};

export async function loadRuntimeConfig(): Promise<void> {
  try {
    const response = await fetch('/app-config.json', { cache: 'no-store' });

    if (!response.ok) {
      throw new Error(`Failed to load runtime config: ${response.status}`);
    }

    runtimeConfig = (await response.json()) as RuntimeConfig;
  } catch (error) {
    console.warn('Runtime config not loaded. Falling back to build-time environment.', error);
    runtimeConfig = {};
  }
}

export function getRuntimeConfig(): RuntimeConfig {
  return runtimeConfig;
}
