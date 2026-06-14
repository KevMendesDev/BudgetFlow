import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AuthApiService } from '../../../../core/services/auth-api.service';
import { fieldError } from '../../../../shared/utils/form-error.util';
import { mapApiError } from '../../../../shared/utils/error-message.util';

@Component({
  selector: 'app-verify-email-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './verify-email-page.component.html',
})
export class VerifyEmailPageComponent {
  private readonly authApi = inject(AuthApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly formBuilder = inject(FormBuilder);

  readonly loading = signal(false);
  readonly message = signal('Enviamos um link de confirmação. Confira também a caixa de spam.');
  readonly errorMessage = signal('');
  readonly fieldError = fieldError;

  readonly form = this.formBuilder.nonNullable.group({
    email: [
      this.route.snapshot.queryParamMap.get('email') ?? '',
      [Validators.required, Validators.email],
    ],
  });

  resend(): void {
    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');
    this.authApi
      .resendVerification(this.form.controls.email.value.trim().toLowerCase())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.message.set(response.message);
          this.loading.set(false);
        },
        error: (error) => {
          this.errorMessage.set(mapApiError(error));
          this.loading.set(false);
        },
      });
  }
}
