import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthApiService } from '../../../../core/services/auth-api.service';
import { SessionService } from '../../../../core/services/session.service';
import { formatCpf, onlyDigits } from '../../../../shared/utils/format.util';
import { fieldError } from '../../../../shared/utils/form-error.util';
import { cpfValidator } from '../../../../shared/validators/br-validators';
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
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly fieldError = fieldError;

  readonly form = this.formBuilder.nonNullable.group({
    cpf: ['', [Validators.required, cpfValidator()]],
    senha: ['', [Validators.required]],
  });

  onCpfInput(): void {
    const cpfControl = this.form.controls.cpf;
    cpfControl.setValue(formatCpf(cpfControl.value), { emitEvent: false });
  }

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
        cpf: onlyDigits(formValue.cpf),
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
