import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthApiService } from '../../../../core/services/auth-api.service';
import { fieldError } from '../../../../shared/utils/form-error.util';
import { mapApiError } from '../../../../shared/utils/error-message.util';
import { strongPasswordValidator } from '../../../../shared/validators/br-validators';

@Component({
  selector: 'app-reset-password-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password-page.component.html',
})
export class ResetPasswordPageComponent {
  private readonly authApi = inject(AuthApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly fieldError = fieldError;
  readonly token = this.route.snapshot.queryParamMap.get('token') ?? '';

  readonly form = this.formBuilder.nonNullable.group({
    senha: ['', [Validators.required, Validators.minLength(8), strongPasswordValidator()]],
    confirmarSenha: ['', [Validators.required]],
  });

  submit(): void {
    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    if (value.senha !== value.confirmarSenha) {
      this.errorMessage.set('As senhas não coincidem.');
      return;
    }
    if (!this.token) {
      this.errorMessage.set('Link de recuperação inválido.');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');
    this.authApi
      .resetPassword(this.token, value.senha)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.router.navigate(['/login'], { queryParams: { message: response.message } });
        },
        error: (error) => {
          this.errorMessage.set(mapApiError(error));
          this.loading.set(false);
        },
      });
  }
}
