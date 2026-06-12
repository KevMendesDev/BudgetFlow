import { TestBed } from '@angular/core/testing';

import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  let darkPreference = false;
  let mediaChangeListener: ((event: MediaQueryListEvent) => void) | null = null;

  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    document.documentElement.style.colorScheme = '';
    darkPreference = false;
    mediaChangeListener = null;

    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: () => ({
        matches: darkPreference,
        media: '(prefers-color-scheme: dark)',
        onchange: null,
        addEventListener: (_type: string, listener: (event: MediaQueryListEvent) => void) => {
          mediaChangeListener = listener;
        },
        removeEventListener: () => {},
        addListener: (listener: (event: MediaQueryListEvent) => void) => {
          mediaChangeListener = listener;
        },
        removeListener: () => {},
        dispatchEvent: () => true,
      }),
    });

    TestBed.configureTestingModule({});
  });

  it('usa system por padrao quando nao ha preferencia salva', () => {
    const service = TestBed.inject(ThemeService);
    TestBed.flushEffects();

    expect(service.mode()).toBe('system');
    expect(service.effectiveTheme()).toBe('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });

  it('persiste a preferencia selecionada', () => {
    const service = TestBed.inject(ThemeService);

    service.setMode('dark');
    TestBed.flushEffects();

    expect(localStorage.getItem('budgetflow.theme-mode')).toBe('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  it('resolve o modo system conforme a preferencia do sistema', () => {
    darkPreference = true;
    const service = TestBed.inject(ThemeService);
    TestBed.flushEffects();

    expect(service.effectiveTheme()).toBe('dark');

    darkPreference = false;
    mediaChangeListener?.({ matches: false } as MediaQueryListEvent);
    TestBed.flushEffects();

    expect(service.effectiveTheme()).toBe('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });
});
