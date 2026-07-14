import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { CsrfTokenService } from './csrf-token.service';
import { SessionResumeService } from './session-resume.service';
import { SessionService } from './session.service';

describe('SessionResumeService', () => {
  let csrfTokenService: { invalidate: ReturnType<typeof vi.fn> };
  let session: {
    isAuthenticated: ReturnType<typeof vi.fn>;
    bootstrapSession: ReturnType<typeof vi.fn>;
  };
  let service: SessionResumeService;

  beforeEach(() => {
    csrfTokenService = { invalidate: vi.fn() };
    session = {
      isAuthenticated: vi.fn(() => false),
      bootstrapSession: vi.fn(() => of(true)),
    };

    TestBed.configureTestingModule({
      providers: [
        SessionResumeService,
        { provide: CsrfTokenService, useValue: csrfTokenService },
        { provide: SessionService, useValue: session },
      ],
    });

    service = TestBed.inject(SessionResumeService);
  });

  it('invalida CSRF ao retomar', () => {
    service.onResume();

    expect(csrfTokenService.invalidate).toHaveBeenCalledTimes(1);
    expect(session.bootstrapSession).not.toHaveBeenCalled();
  });

  it('revalida a sessao quando ja autenticado em memoria', () => {
    session.isAuthenticated.mockReturnValue(true);

    service.onResume();

    expect(csrfTokenService.invalidate).toHaveBeenCalledTimes(1);
    expect(session.bootstrapSession).toHaveBeenCalledWith(true);
  });

  it('invalida CSRF quando a aba volta a ficar visivel', () => {
    service.start();

    Object.defineProperty(document, 'visibilityState', {
      configurable: true,
      get: () => 'visible',
    });

    document.dispatchEvent(new Event('visibilitychange'));

    expect(csrfTokenService.invalidate).toHaveBeenCalledTimes(1);
  });
});
