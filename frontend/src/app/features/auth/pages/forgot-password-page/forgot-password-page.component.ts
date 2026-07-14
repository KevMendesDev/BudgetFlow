import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subscription, interval } from 'rxjs';

import { AuthApiService } from '../../../../core/services/auth-api.service';
import { fieldError } from '../../../../shared/utils/form-error.util';
import { mapApiError } from '../../../../shared/utils/error-message.util';

@Component({
  selector: 'app-forgot-password-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password-page.component.html',
})
export class ForgotPasswordPageComponent {
  private static readonly COOLDOWN_SECONDS = 60;

  private readonly authApi = inject(AuthApiService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private cooldownSub?: Subscription;

  readonly loading = signal(false);
  readonly cooldownSeconds = signal(0);
  readonly message = signal('');
  readonly errorMessage = signal('');
  readonly fieldError = fieldError;
  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  constructor() {
    this.destroyRef.onDestroy(() => this.cooldownSub?.unsubscribe());
  }

  submit(): void {
    if (this.form.invalid || this.loading() || this.cooldownSeconds() > 0) {
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
          this.startCooldown();
        },
        error: (error) => {
          this.errorMessage.set(mapApiError(error));
          this.loading.set(false);
        },
      });
  }

  private startCooldown(): void {
    this.cooldownSub?.unsubscribe();
    this.cooldownSeconds.set(ForgotPasswordPageComponent.COOLDOWN_SECONDS);
    this.cooldownSub = interval(1000).subscribe(() => {
      const next = this.cooldownSeconds() - 1;
      this.cooldownSeconds.set(Math.max(0, next));
      if (next <= 0) {
        this.cooldownSub?.unsubscribe();
        this.cooldownSub = undefined;
      }
    });
  }
}
