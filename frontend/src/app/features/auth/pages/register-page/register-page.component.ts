import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthApiService } from '../../../../core/services/auth-api.service';
import { SessionService } from '../../../../core/services/session.service';
import { mapApiError } from '../../../../shared/utils/error-message.util';
import { formatCpf, formatPhone, onlyDigits } from '../../../../shared/utils/format.util';
import { cpfValidator, strongPasswordValidator, telefoneValidator } from '../../../../shared/validators/br-validators';

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register-page.component.html',
})
export class RegisterPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authApi = inject(AuthApiService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly errorMessage = signal('');

  readonly form = this.formBuilder.nonNullable.group({
    nome: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    cpf: ['', [Validators.required, cpfValidator()]],
    telefone: ['', [telefoneValidator(true)]],
    senha: ['', [Validators.required, Validators.minLength(8), strongPasswordValidator()]],
  });

  onCpfInput(): void {
    const cpfControl = this.form.controls.cpf;
    cpfControl.setValue(formatCpf(cpfControl.value), { emitEvent: false });
  }

  onPhoneInput(): void {
    const telefoneControl = this.form.controls.telefone;
    telefoneControl.setValue(formatPhone(telefoneControl.value), { emitEvent: false });
  }

  fieldError(control: AbstractControl | null, label: string): string {
    if (!control || !control.touched) {
      return '';
    }

    if (control.hasError('required')) {
      return `${label} obrigatorio`;
    }

    if (control.hasError('email')) {
      return 'Email invalido';
    }

    if (control.hasError('cpf')) {
      return 'CPF invalido';
    }

    if (control.hasError('telefone')) {
      return 'Telefone invalido';
    }

    if (control.hasError('strongPassword')) {
      return 'Senha fraca: usa maiuscula, minuscula, numero e especial';
    }

    if (control.hasError('minlength')) {
      return 'Senha precisa ter pelo menos 8 caracteres';
    }

    return '';
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
      .register({
        ...formValue,
        cpf: onlyDigits(formValue.cpf),
        telefone: formValue.telefone ? onlyDigits(formValue.telefone) : null,
      })
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
