import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AuthApiService } from '../../core/services/auth-api.service';
import { SessionService } from '../../core/services/session.service';
import { PrivateLayoutComponent } from './private-layout.component';

@Component({
  template: '',
  standalone: true,
})
class DummyComponent {}

describe('PrivateLayoutComponent', () => {
  beforeEach(async () => {
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: (query: string) => ({
        matches: query === '(min-width: 961px)',
        media: query,
        onchange: null,
        addEventListener: () => {},
        removeEventListener: () => {},
        addListener: () => {},
        removeListener: () => {},
        dispatchEvent: () => true,
      }),
    });

    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    document.documentElement.style.colorScheme = '';

    await TestBed.configureTestingModule({
      imports: [PrivateLayoutComponent],
      providers: [
        provideRouter([
          { path: 'dashboard', component: DummyComponent },
          { path: 'categorias', component: DummyComponent },
          { path: 'transacoes-recorrentes', component: DummyComponent },
          { path: 'periodos-financeiros', component: DummyComponent },
          { path: 'login', component: DummyComponent },
        ]),
        {
          provide: SessionService,
          useValue: {
            user: signal({ nome: 'Maria' }),
            clearSession: () => {},
          },
        },
        {
          provide: AuthApiService,
          useValue: {
            logout: () => of(void 0),
          },
        },
      ],
    }).compileComponents();
  });

  it('renderiza o controle de tema', () => {
    const fixture = TestBed.createComponent(PrivateLayoutComponent);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('app-theme-menu')).toBeTruthy();
    expect(element.querySelectorAll('.theme-options button').length).toBe(3);
  });

  it('troca o tema ao clicar na opcao', () => {
    const fixture = TestBed.createComponent(PrivateLayoutComponent);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const darkButton = Array.from(element.querySelectorAll<HTMLButtonElement>('.theme-options button')).find((button) =>
      button.textContent?.includes('Escuro')
    );

    darkButton?.click();
    fixture.detectChanges();
    TestBed.flushEffects();

    expect(localStorage.getItem('budgetflow.theme-mode')).toBe('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });
});
