import { ChartType, TooltipItem } from 'chart.js';

import { formatMoney } from './format.util';

/** Label de tooltip Chart.js com valor em R$ (pt-BR). */
export function currencyTooltipLabel(context: TooltipItem<ChartType>): string {
  const label = context.dataset.label || context.label || '';
  const value = resolveTooltipValue(context);
  const prefix = label ? `${label}: ` : '';
  return `${prefix}${formatMoney(value)}`;
}

function resolveTooltipValue(context: TooltipItem<ChartType>): number {
  const parsed = context.parsed;

  if (typeof parsed === 'number') {
    return parsed;
  }

  if (parsed && typeof parsed === 'object' && 'y' in parsed) {
    return Number((parsed as { y: number }).y) || 0;
  }

  return Number(context.raw) || 0;
}
