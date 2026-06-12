import { ChartData } from 'chart.js';

import { ClassificacaoCategoria } from '../../../core/models/categoria.models';
import { PeriodoFinanceiro } from '../../../core/models/periodo-financeiro.models';
import { TransacaoResponse } from '../../../core/models/transacao.models';
import { formatDate, toIsoDate } from '../../../shared/utils/format.util';

export interface CategoriaResumo {
  nome: string;
  classificacao: ClassificacaoCategoria;
  total: number;
  percentualSaldo: number;
  percentualLucro: number;
  color: string;
}

export interface ClassificacaoResumo {
  classificacao: ClassificacaoCategoria;
  total: number;
  widthPercent: number;
}

export function filterDespesas(transacoes: TransacaoResponse[]): TransacaoResponse[] {
  return transacoes.filter((tx) => tx.tipoMovimentacao === 'DESPESA');
}

export function sumValores(transacoes: TransacaoResponse[]): number {
  return transacoes.reduce((sum, tx) => sum + Number(tx.valor), 0);
}

export function buildColorMap(keys: string[], colorFromIndex: (index: number) => string): Map<string, string> {
  const map = new Map<string, string>();
  Array.from(new Set(keys))
    .sort((a, b) => a.localeCompare(b))
    .forEach((key, index) => map.set(key, colorFromIndex(index)));
  return map;
}

export function buildResumoCategorias(
  despesas: TransacaoResponse[],
  saldo: number,
  totalReceitas: number,
  colorForKey: (key: string) => string
): CategoriaResumo[] {
  const saldoBase = Math.max(1, Math.abs(saldo));
  const lucroBase = Math.max(1, totalReceitas);
  const mapa = new Map<string, CategoriaResumo>();

  despesas.forEach((tx) => {
    const key = `${tx.categoriaNome}__${tx.classificacaoCategoria}`;
    const atual = mapa.get(key);
    const total = (atual?.total ?? 0) + Number(tx.valor);

    mapa.set(key, {
      nome: tx.categoriaNome,
      classificacao: tx.classificacaoCategoria as ClassificacaoCategoria,
      total,
      percentualSaldo: Math.min(100, (total / saldoBase) * 100),
      percentualLucro: Math.min(100, (total / lucroBase) * 100),
      color: colorForKey(tx.categoriaNome),
    });
  });

  return Array.from(mapa.values()).sort((a, b) => b.total - a.total);
}

export function buildResumoClassificacao(despesas: TransacaoResponse[]): ClassificacaoResumo[] {
  const mapa = new Map<ClassificacaoCategoria, number>();

  despesas.forEach((tx) => {
    const classificacao = tx.classificacaoCategoria as ClassificacaoCategoria;
    mapa.set(classificacao, (mapa.get(classificacao) ?? 0) + Number(tx.valor));
  });

  const totais = Array.from(mapa.entries()).map(([classificacao, total]) => ({
    classificacao,
    total,
  }));
  const maxTotal = Math.max(1, ...totais.map((item) => item.total));

  return totais
    .map((item) => ({
      ...item,
      widthPercent: (item.total / maxTotal) * 100,
    }))
    .sort((a, b) => b.total - a.total);
}

export function buildCategoriasLegenda(
  despesas: TransacaoResponse[],
  colorForKey: (key: string) => string
): Array<{ nome: string; color: string }> {
  return Array.from(new Set(despesas.map((tx) => tx.categoriaNome))).map((nome) => ({
    nome,
    color: colorForKey(nome),
  }));
}

export function buildDailyChartData(
  periodo: PeriodoFinanceiro | null,
  despesas: TransacaoResponse[],
  colorForKey: (key: string) => string
): ChartData<'bar'> {
  if (!periodo) {
    return { labels: [], datasets: [] };
  }

  const dias = getDiasNoPeriodo(periodo.dataInicio, periodo.dataFim);
  const labels = dias.map((dia) => formatDate(dia));
  const categorias = Array.from(new Set(despesas.map((tx) => tx.categoriaNome)));

  return {
    labels,
    datasets: categorias.map((categoria) => ({
      label: categoria,
      data: dias.map((dia) =>
        sumValores(despesas.filter((tx) => tx.data === dia && tx.categoriaNome === categoria))
      ),
      backgroundColor: colorForKey(categoria),
      borderRadius: 6,
      borderSkipped: false as const,
    })),
  };
}

export function buildClassificacaoChartData(
  resumoClassificacao: ClassificacaoResumo[],
  classificacaoLabel: (classificacao: ClassificacaoCategoria) => string,
  colorForKey: (key: string) => string
): ChartData<'doughnut'> {
  return {
    labels: resumoClassificacao.map((item) => classificacaoLabel(item.classificacao)),
    datasets: [
      {
        data: resumoClassificacao.map((item) => item.total),
        backgroundColor: resumoClassificacao.map((item) => colorForKey(item.classificacao)),
        borderWidth: 0,
      },
    ],
  };
}

export function buildCategoriaChartData(
  totalCategoria: number,
  totalReceitas: number,
  itemColor: string,
  trackColor: string
): ChartData<'doughnut'> {
  const receitas = Math.max(1, totalReceitas);
  const restante = Math.max(0, receitas - totalCategoria);

  return {
    labels: ['Gasto', 'Restante'],
    datasets: [
      {
        data: [totalCategoria, restante],
        backgroundColor: [itemColor, trackColor],
        borderWidth: 0,
      },
    ],
  };
}

function getDiasNoPeriodo(inicio: string, fim: string): string[] {
  const dias: string[] = [];
  let atual = new Date(`${inicio}T00:00:00`);
  const end = new Date(`${fim}T00:00:00`);

  while (atual <= end) {
    dias.push(toIsoDate(atual));
    atual.setDate(atual.getDate() + 1);
  }

  return dias;
}
