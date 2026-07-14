import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of } from 'rxjs';

import { AuthApiService } from '../../../../core/services/auth-api.service';
import { ResetPasswordPageComponent } from './reset-password-page.component';

class AuthApiMock {
  calls: Array<{ token: string; senha: string }> = [];

  resetPassword(token: string, senha: string) {
    this.calls.push({ token, senha });
    return of({ message: 'Senha alterada. Entre novamente.' });
  }
}

class RouterMock {
  navigations: unknown[][] = [];

  navigate(commands: unknown[], extras?: unknown) {
    this.navigations.push([commands, extras]);
    return Promise.resolve(true);
  }
}

describe('ResetPasswordPageComponent', () => {
  let authApi: AuthApiMock;
  let router: RouterMock;

  beforeEach(async () => {
    authApi = new AuthApiMock();
    router = new RouterMock();

    await TestBed.configureTestingModule({
      imports: [ResetPasswordPageComponent],
      providers: [
        { provide: AuthApiService, useValue: authApi },
        { provide: Router, useValue: router },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap({ token: 'valid-token' }) } },
        },
      ],
    }).compileComponents();
  });

  it('não envia quando as senhas são diferentes', () => {
    const component = TestBed.createComponent(ResetPasswordPageComponent).componentInstance;
    component.form.setValue({ senha: 'Secret@123', confirmarSenha: 'Other@123' });

    component.submit();

    expect(authApi.calls).toHaveLength(0);
    expect(component.errorMessage()).toBe('As senhas não coincidem.');
  });

  it('redefine e redireciona para o login', () => {
    const component = TestBed.createComponent(ResetPasswordPageComponent).componentInstance;
    component.form.setValue({ senha: 'Secret@123', confirmarSenha: 'Secret@123' });

    component.submit();

    expect(authApi.calls).toEqual([{ token: 'valid-token', senha: 'Secret@123' }]);
    expect(router.navigations[0][0]).toEqual(['/login']);
  });
});
