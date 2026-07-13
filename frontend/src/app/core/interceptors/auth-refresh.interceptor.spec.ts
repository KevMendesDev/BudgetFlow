import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { CsrfTokenService } from '../services/csrf-token.service';
import { RawHttpService } from '../services/raw-http.service';
import { SessionService } from '../services/session.service';
import {
  authRefreshInterceptor,
  CSRF_RECOVERY_ATTEMPTED,
  resetAuthRefreshStateForTests,
} from './auth-refresh.interceptor';

describe('authRefreshInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let csrfTokenService: CsrfTokenService;
  let session: { clearSession: ReturnType<typeof vi.fn> };
  let router: Router;

  beforeEach(() => {
    resetAuthRefreshStateForTests();
    session = { clearSession: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authRefreshInterceptor])),
        provideHttpClientTesting(),
        {
          provide: SessionService,
          useValue: session,
        },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    csrfTokenService = TestBed.inject(CsrfTokenService);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    httpMock.verify();
    csrfTokenService.invalidate();
    resetAuthRefreshStateForTests();
  });

  it('em 403 invalida CSRF, busca token novo e retenta com header atualizado', async () => {
    const invalidateSpy = vi.spyOn(csrfTokenService, 'invalidate');
    const requestPromise = firstValueFrom(
      http.post('/api/categorias', { nome: 'Moradia' }, {
        headers: { 'X-XSRF-TOKEN': 'token-antigo' },
      })
    );

    const first = httpMock.expectOne('/api/categorias');
    expect(first.request.headers.get('X-XSRF-TOKEN')).toBe('token-antigo');
    first.flush({ error: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });

    const csrfReq = httpMock.expectOne('/api/auth/csrf');
    csrfReq.flush({ token: 'token-novo', headerName: 'X-XSRF-TOKEN' });

    const retry = httpMock.expectOne('/api/categorias');
    expect(retry.request.headers.get('X-XSRF-TOKEN')).toBe('token-novo');
    expect(retry.request.context.get(CSRF_RECOVERY_ATTEMPTED)).toBe(true);
    retry.flush({ id: 1 });

    await expect(requestPromise).resolves.toEqual({ id: 1 });
    expect(invalidateSpy).toHaveBeenCalled();
  });

  it('em 403 no retry propaga o erro sem loop', async () => {
    const requestPromise = firstValueFrom(
      http.post('/api/categorias', {}, { headers: { 'X-XSRF-TOKEN': 'token-antigo' } })
    ).catch((error: unknown) => error);

    httpMock.expectOne('/api/categorias').flush(null, { status: 403, statusText: 'Forbidden' });
    httpMock.expectOne('/api/auth/csrf').flush({ token: 'token-novo', headerName: 'X-XSRF-TOKEN' });

    const retry = httpMock.expectOne('/api/categorias');
    expect(retry.request.context.get(CSRF_RECOVERY_ATTEMPTED)).toBe(true);
    retry.flush(null, { status: 403, statusText: 'Forbidden' });

    const error = (await requestPromise) as HttpErrorResponse;
    expect(error).toBeInstanceOf(HttpErrorResponse);
    expect(error.status).toBe(403);
    httpMock.expectNone('/api/auth/csrf');
  });

  it('em 401 invalida CSRF, faz refresh e retenta com header fresco', async () => {
    const invalidateSpy = vi.spyOn(csrfTokenService, 'invalidate');
    const requestPromise = firstValueFrom(
      http.post('/api/categorias', {}, { headers: { 'X-XSRF-TOKEN': 'token-antigo' } })
    );

    httpMock.expectOne('/api/categorias').flush(null, { status: 401, statusText: 'Unauthorized' });

    httpMock.expectOne('/api/auth/csrf').flush({ token: 'csrf-refresh', headerName: 'X-XSRF-TOKEN' });

    const refresh = httpMock.expectOne('/api/auth/refresh');
    expect(refresh.request.headers.get('X-XSRF-TOKEN')).toBe('csrf-refresh');
    refresh.flush(null);

    httpMock.expectOne('/api/auth/csrf').flush({ token: 'csrf-pos-refresh', headerName: 'X-XSRF-TOKEN' });

    const retry = httpMock.expectOne('/api/categorias');
    expect(retry.request.headers.get('X-XSRF-TOKEN')).toBe('csrf-pos-refresh');
    retry.flush({ id: 2 });

    await expect(requestPromise).resolves.toEqual({ id: 2 });
    expect(invalidateSpy).toHaveBeenCalled();
    expect(session.clearSession).not.toHaveBeenCalled();
  });

  it('quando o refresh falha limpa a sessao e navega para login', async () => {
    const requestPromise = firstValueFrom(http.get('/api/auth/me')).catch((error: unknown) => error);

    httpMock.expectOne('/api/auth/me').flush(null, { status: 401, statusText: 'Unauthorized' });
    httpMock.expectOne('/api/auth/csrf').flush({ token: 'csrf', headerName: 'X-XSRF-TOKEN' });
    httpMock.expectOne('/api/auth/refresh').flush(
      { error: 'Refresh token inválido' },
      { status: 401, statusText: 'Unauthorized' }
    );

    const error = (await requestPromise) as HttpErrorResponse;
    expect(error.status).toBe(401);
    expect(session.clearSession).toHaveBeenCalledTimes(1);
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
