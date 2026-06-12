import { DOCUMENT } from '@angular/common';
import { computed, effect, inject, Injectable, signal } from '@angular/core';

export type ThemeMode = 'light' | 'dark' | 'system';
export type ResolvedTheme = Exclude<ThemeMode, 'system'>;

const STORAGE_KEY = 'budgetflow.theme-mode';
const MEDIA_QUERY = '(prefers-color-scheme: dark)';

function isThemeMode(value: string | null): value is ThemeMode {
  return value === 'light' || value === 'dark' || value === 'system';
}

function getWindowRef(): Window | null {
  return typeof window === 'undefined' ? null : window;
}

function getMediaQueryList(win = getWindowRef()): MediaQueryList | null {
  return typeof win?.matchMedia === 'function' ? win.matchMedia(MEDIA_QUERY) : null;
}

function resolveSystemTheme(win = getWindowRef()): ResolvedTheme {
  return getMediaQueryList(win)?.matches ? 'dark' : 'light';
}

function readStoredMode(storage = getWindowRef()?.localStorage): ThemeMode {
  const stored = storage?.getItem(STORAGE_KEY) ?? null;
  return isThemeMode(stored) ? stored : 'system';
}

function writeStoredMode(mode: ThemeMode, storage = getWindowRef()?.localStorage): void {
  storage?.setItem(STORAGE_KEY, mode);
}

export function applyResolvedTheme(theme: ResolvedTheme, doc = document): void {
  doc.documentElement.setAttribute('data-theme', theme);
  doc.documentElement.style.colorScheme = theme;
}

export function applyInitialTheme(): void {
  if (typeof document === 'undefined') {
    return;
  }

  const mode = readStoredMode();
  const theme = mode === 'system' ? resolveSystemTheme() : mode;
  applyResolvedTheme(theme, document);
}

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly mediaQuery = getMediaQueryList();
  private readonly systemTheme = signal<ResolvedTheme>(resolveSystemTheme());

  readonly mode = signal<ThemeMode>(readStoredMode());
  readonly effectiveTheme = computed<ResolvedTheme>(() => {
    const mode = this.mode();
    return mode === 'system' ? this.systemTheme() : mode;
  });

  constructor() {
    this.bindSystemThemeListener();

    effect(() => {
      writeStoredMode(this.mode());
    });

    effect(() => {
      applyResolvedTheme(this.effectiveTheme(), this.document);
    });
  }

  setMode(mode: ThemeMode): void {
    this.mode.set(mode);
  }

  private bindSystemThemeListener(): void {
    if (!this.mediaQuery) {
      return;
    }

    const syncTheme = (event?: MediaQueryListEvent): void => {
      const prefersDark = event?.matches ?? this.mediaQuery?.matches ?? false;
      this.systemTheme.set(prefersDark ? 'dark' : 'light');
    };

    syncTheme();

    if ('addEventListener' in this.mediaQuery) {
      this.mediaQuery.addEventListener('change', syncTheme);
      return;
    }

    (this.mediaQuery as MediaQueryList & { addListener?: typeof syncTheme }).addListener?.(syncTheme);
  }
}
