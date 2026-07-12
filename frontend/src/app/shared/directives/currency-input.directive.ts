import { Directive, ElementRef, HostListener, inject } from '@angular/core';
import { NgControl } from '@angular/forms';

import { formatCurrencyInput } from '../utils/format.util';

@Directive({
  selector: 'input[appCurrencyInput]',
  standalone: true,
})
export class CurrencyInputDirective {
  private readonly el = inject(ElementRef<HTMLInputElement>);
  private readonly ngControl = inject(NgControl, { optional: true, self: true });

  @HostListener('input')
  onInput(): void {
    const formatted = formatCurrencyInput(this.el.nativeElement.value);
    this.el.nativeElement.value = formatted;
    this.ngControl?.control?.setValue(formatted, { emitEvent: false });
  }
}
