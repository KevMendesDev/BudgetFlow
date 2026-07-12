import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

import { parseCurrencyInput } from '../utils/format.util';

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

export function currencyAmountValidator(min = 0.01, optional = false): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const raw = String(control.value ?? '').trim();
    if (!raw) {
      return optional ? null : { required: true };
    }

    const amount = parseCurrencyInput(raw);
    if (amount == null) {
      return { required: true };
    }

    if (amount < min) {
      return { min: { min, actual: amount } };
    }

    return null;
  };
}
