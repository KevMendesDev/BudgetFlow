import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AuthApiService } from '../../../../core/services/auth-api.service';
import { fieldError } from '../../../../shared/utils/form-error.util';
import { mapApiError } from '../../../../shared/utils/error-message.util';

@Component({
  selector: 'app-forgot-password-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password-page.component.html',
})
export class ForgotPasswordPageComponent {
  private readonly authApi = inject(AuthApiService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(false);
  readonly message = signal('');
  readonly errorMessage = signal('');
  readonly fieldError = fieldError;
  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  submit(): void {
    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');
    this.authApi
      .forgotPassword(this.form.controls.email.value.trim().toLowerCase())
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
