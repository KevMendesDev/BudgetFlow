import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { SessionService } from '../services/session.service';
import { guestGuard } from './guest.guard';

describe('guestGuard', () => {
  it('não faz bootstrap de sessão em rotas públicas', async () => {
    const bootstrapSession = vi.fn();

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: SessionService,
          useValue: {
            isAuthenticated: () => false,
            bootstrapSession,
          },
        },
      ],
    });

    const result = await TestBed.runInInjectionContext(() => guestGuard({} as never, {} as never));

    expect(result).toBe(true);
    expect(bootstrapSession).not.toHaveBeenCalled();
  });

  it('redireciona para dashboard quando já existe sessão em memória', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: SessionService,
          useValue: {
            isAuthenticated: () => true,
          },
        },
      ],
    });

    const router = TestBed.inject(Router);
    const result = await TestBed.runInInjectionContext(() => guestGuard({} as never, {} as never));

    expect(result).toEqual(router.createUrlTree(['/dashboard']));
  });
});
