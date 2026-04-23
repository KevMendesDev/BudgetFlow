import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

function normalizeDigits(value: unknown): string {
  return String(value ?? '').replace(/\D/g, '');
}

export function cpfValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const cpf = normalizeDigits(control.value);

    if (!cpf) {
      return null;
    }

    if (cpf.length !== 11 || /^(\d)\1{10}$/.test(cpf)) {
      return { cpf: true };
    }

    const digits = cpf.split('').map(Number);

    const dv1 = calculateDigit(digits.slice(0, 9), 10);
    const dv2 = calculateDigit(digits.slice(0, 10), 11);

    if (dv1 !== digits[9] || dv2 !== digits[10]) {
      return { cpf: true };
    }

    return null;
  };
};

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

function calculateDigit(baseDigits: number[], factor: number): number {
  const total = baseDigits.reduce((sum, digit, index) => {
    return sum + digit * (factor - index);
  }, 0);

  const remainder = total % 11;
  return remainder < 2 ? 0 : 11 - remainder;
}
