import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { API_BASE_URL } from '../../../../core/config/api.config';
import { AuthApiService } from '../../../../core/services/auth-api.service';
import { SessionService } from '../../../../core/services/session.service';
import { fieldError } from '../../../../shared/utils/form-error.util';
import { mapApiError } from '../../../../shared/utils/error-message.util';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login-page.component.html',
})
export class LoginPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authApi = inject(AuthApiService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(false);
  readonly errorMessage = signal(
    this.route.snapshot.queryParamMap.get('googleError')
      ? 'Não foi possível entrar com o Google.'
      : ''
  );
  readonly fieldError = fieldError;
  readonly googleLoginUrl = `${API_BASE_URL}/oauth2/authorization/google`;

  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required]],
  });

  submit(): void {
    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage.set('');
    this.loading.set(true);

    const formValue = this.form.getRawValue();

    this.authApi
      .login({
        email: formValue.email.trim().toLowerCase(),
        senha: formValue.senha,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (currentUser) => {
          this.session.setUser(currentUser);
          this.loading.set(false);
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.errorMessage.set(mapApiError(err));
          this.loading.set(false);
        },
      });
  }
}
