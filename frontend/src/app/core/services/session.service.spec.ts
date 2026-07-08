import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { CurrentUser } from '../models/auth.models';
import { AuthApiService } from './auth-api.service';
import { CsrfTokenService } from './csrf-token.service';
import { SessionService } from './session.service';

describe('SessionService', () => {
  const currentUser: CurrentUser = {
    id: 1,
    nome: 'Teste',
    email: 'teste@budgetflow.com',
    roles: ['USER'],
  };

  let csrfTokenService: { invalidate: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    csrfTokenService = {
      invalidate: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        SessionService,
        {
          provide: AuthApiService,
          useValue: {
            me: () => of(currentUser),
          },
        },
        {
          provide: CsrfTokenService,
          useValue: csrfTokenService,
        },
      ],
    });
  });

  it('invalida o cache de csrf ao autenticar o usuario', () => {
    const service = TestBed.inject(SessionService);

    service.setUser(currentUser);

    expect(csrfTokenService.invalidate).toHaveBeenCalledTimes(1);
    expect(service.user()).toEqual(currentUser);
    expect(service.isAuthenticated()).toBe(true);
  });

  it('invalida o cache de csrf ao limpar a sessao', () => {
    const service = TestBed.inject(SessionService);
    service.setUser(currentUser);

    service.clearSession();

    expect(csrfTokenService.invalidate).toHaveBeenCalledTimes(2);
    expect(service.user()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
  });
});
