import { ChartType, TooltipItem } from 'chart.js';

import { formatMoney } from './format.util';

export function currencyTooltipLabel<TType extends ChartType>(context: TooltipItem<TType>): string {
  const dataset = context.dataset as { label?: string };
  const label = dataset.label || context.label || '';
  const value = resolveTooltipValue(context);
  const prefix = label ? `${label}: ` : '';
  return `${prefix}${formatMoney(value)}`;
}

function resolveTooltipValue<TType extends ChartType>(context: TooltipItem<TType>): number {
  const parsed = context.parsed as number | { y?: number } | null | undefined;

  if (typeof parsed === 'number') {
    return parsed;
  }

  if (parsed && typeof parsed === 'object' && 'y' in parsed) {
    return Number(parsed.y) || 0;
  }

  return Number(context.raw) || 0;
}
