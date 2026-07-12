export function onlyDigits(value: string): string {
  return value.replace(/\D/g, '');
}

export function formatPhone(value: string): string {
  const digits = onlyDigits(value).slice(0, 11);

  if (digits.length <= 10) {
    return digits
      .replace(/(\d{2})(\d)/, '($1) $2')
      .replace(/(\d{4})(\d{1,4})$/, '$1-$2');
  }

  return digits
    .replace(/(\d{2})(\d)/, '($1) $2')
    .replace(/(\d{5})(\d{1,4})$/, '$1-$2');
}

const moneyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
  minimumFractionDigits: 2,
});

const monthFormatter = new Intl.DateTimeFormat('pt-BR', { month: 'long', timeZone: 'UTC' });

export const MONTH_OPTIONS = Array.from({ length: 12 }, (_, index) => ({
  value: index + 1,
  label: formatMonthName(index + 1),
}));

export function formatMoney(value: number): string {
  return moneyFormatter.format(value);
}

export function formatCurrencyInput(value: string): string {
  const digits = onlyDigits(value).slice(0, 15);
  if (!digits) {
    return '';
  }

  return formatMoney(Number(digits) / 100);
}

/** Converte "R$ 1.234,56" (ou número) em number; vazio → null. */
export function parseCurrencyInput(value: string | number | null | undefined): number | null {
  if (value == null || value === '') {
    return null;
  }

  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : null;
  }

  const digits = onlyDigits(value);
  if (!digits) {
    return null;
  }

  return Number(digits) / 100;
}

export function toCurrencyInputValue(value: number | null | undefined): string {
  if (value == null || !Number.isFinite(value)) {
    return '';
  }

  return formatMoney(value);
}

export function formatDate(value: string): string {
  return new Date(`${value}T00:00:00`).toLocaleDateString('pt-BR');
}

export function formatDateOrNull(value: string | null): string {
  if (!value) return '-';
  return formatDate(value);
}

export function formatMonthYear(month: number, year: number): string {
  const label = formatMonthName(month);
  return `${label}/${year}`;
}

export function formatMonthName(month: number): string {
  const label = monthFormatter.format(new Date(Date.UTC(2000, month - 1, 1)));
  return `${label.charAt(0).toUpperCase()}${label.slice(1)}`;
}

export function toIsoDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}
