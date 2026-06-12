import { Pipe, PipeTransform } from '@angular/core';

import { formatMoney } from '../utils/format.util';

@Pipe({ name: 'currencyBRL', standalone: true })
export class CurrencyBRLPipe implements PipeTransform {
  transform(value: number): string {
    return formatMoney(value);
  }
}
