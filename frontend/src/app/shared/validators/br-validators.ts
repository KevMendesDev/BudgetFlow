import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

function normalizeDigits(value: unknown): string {
  return String(value ?? '').replace(/\D/g, '');
}

export function telefoneValidator(optional = false): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const phone = normalizeDigits(control.value);

    if (!phone) {
      return optional ? null : { telefone: true };
    }

    if (phone.length < 10 || phone.length > 11) {
      return { telefone: true };
    }

    return null;
  };
}

export function strongPasswordValidator(): ValidatorFn {
  const regex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_]).{8,}$/;

  return (control: AbstractControl): ValidationErrors | null => {
    const value = String(control.value ?? '');

    if (!value) {
      return null;
    }

    return regex.test(value) ? null : { strongPassword: true };
  };
}
