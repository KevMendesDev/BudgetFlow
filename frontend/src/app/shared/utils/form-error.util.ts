import { AbstractControl } from '@angular/forms';

export function fieldError(control: AbstractControl | null, label: string): string {
  if (!control || !control.touched) {
    return '';
  }

  if (control.hasError('required')) {
    return `${label} obrigatório`;
  }

  if (control.hasError('maxlength')) {
    const max = control.getError('maxlength').requiredLength as number;
    return `${label} deve ter no máximo ${max} caracteres`;
  }

  if (control.hasError('minlength')) {
    const min = control.getError('minlength').requiredLength as number;
    return `${label} precisa ter pelo menos ${min} caracteres`;
  }

  if (control.hasError('min')) {
    return `${label} deve ser maior que zero`;
  }

  if (control.hasError('email')) {
    return 'E-mail inválido';
  }

  if (control.hasError('telefone')) {
    return 'Telefone inválido';
  }

  if (control.hasError('strongPassword')) {
    return 'Senha fraca: use maiúscula, minúscula, número e caractere especial';
  }

  return '';
}
