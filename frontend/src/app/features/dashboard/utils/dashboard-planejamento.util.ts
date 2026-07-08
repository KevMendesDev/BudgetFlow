import { ChartData } from 'chart.js';

import { PlanejamentoResponse } from '../../../core/models/planejamento.models';
import { TransacaoResponse } from '../../../core/models/transacao.models';

export interface ComparativoPlanejamento {
  categorias: string[];
  planejadoPorCategoria: number[];
  executadoPorCategoria: number[];
  totalPlanejado: number;
  totalExecutado: number;
}

export function buildComparativoPlanejamento(
  planejamentos: PlanejamentoResponse[],
  transacoes: TransacaoResponse[]
): ComparativoPlanejamento {
  const planejados = planejamentos.filter((item) => item.tipoMovimentacao === 'DESPESA');
  const executados = transacoes.filter(
    (item) => item.tipoMovimentacao === 'DESPESA' && item.status === 'EXECUTADO'
  );
  const categorias = Array.from(
    new Set([...planejados.map((item) => item.categoriaNome), ...executados.map((item) => item.categoriaNome)])
  ).sort((a, b) => a.localeCompare(b));

  const sumCategoria = (
    items: Array<PlanejamentoResponse | TransacaoResponse>,
    categoria: string
  ): number =>
    items
      .filter((item) => item.categoriaNome === categoria)
      .reduce((total, item) => total + Number(item.valor), 0);

  const planejadoPorCategoria = categorias.map((categoria) => sumCategoria(planejados, categoria));
  const executadoPorCategoria = categorias.map((categoria) => sumCategoria(executados, categoria));

  return {
    categorias,
    planejadoPorCategoria,
    executadoPorCategoria,
    totalPlanejado: planejadoPorCategoria.reduce((total, valor) => total + valor, 0),
    totalExecutado: executadoPorCategoria.reduce((total, valor) => total + valor, 0),
  };
}

export function buildCategoriaComparativoChartData(
  comparativo: ComparativoPlanejamento
): ChartData<'bar'> {
  return {
    labels: comparativo.categorias,
    datasets: [
      { label: 'Planejado', data: comparativo.planejadoPorCategoria, backgroundColor: '#3b82f6' },
      { label: 'Executado', data: comparativo.executadoPorCategoria, backgroundColor: '#12c978' },
    ],
  };
}

export function buildTotaisComparativoChartData(
  comparativo: ComparativoPlanejamento
): ChartData<'bar'> {
  return {
    labels: ['Planejado', 'Executado'],
    datasets: [{
      label: 'Despesas do período',
      data: [comparativo.totalPlanejado, comparativo.totalExecutado],
      backgroundColor: ['#3b82f6', '#12c978'],
    }],
  };
}
