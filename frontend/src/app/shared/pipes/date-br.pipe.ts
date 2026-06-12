import { Pipe, PipeTransform } from '@angular/core';

import { formatDateOrNull } from '../utils/format.util';

@Pipe({ name: 'dateBR', standalone: true })
export class DateBRPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    return formatDateOrNull(value ?? null);
  }
}
