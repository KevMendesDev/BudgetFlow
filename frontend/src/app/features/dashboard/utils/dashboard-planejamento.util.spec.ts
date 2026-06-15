import { PlanejamentoResponse } from '../../../core/models/planejamento.models';
import { TransacaoResponse } from '../../../core/models/transacao.models';
import { buildComparativoPlanejamento } from './dashboard-planejamento.util';

describe('dashboard-planejamento.util', () => {
  it('compara despesas por categoria e ignora transações pendentes', () => {
    const planejamentos = [
      { categoriaNome: 'Mercado', tipoMovimentacao: 'DESPESA', valor: 300 },
      { categoriaNome: 'Lazer', tipoMovimentacao: 'DESPESA', valor: 100 },
      { categoriaNome: 'Salário', tipoMovimentacao: 'RECEITA', valor: 2000 },
    ] as PlanejamentoResponse[];
    const transacoes = [
      { categoriaNome: 'Mercado', tipoMovimentacao: 'DESPESA', status: 'EXECUTADO', valor: 250 },
      { categoriaNome: 'Lazer', tipoMovimentacao: 'DESPESA', status: 'PENDENTE', valor: 80 },
      { categoriaNome: 'Moradia', tipoMovimentacao: 'DESPESA', status: 'EXECUTADO', valor: 500 },
    ] as TransacaoResponse[];

    const result = buildComparativoPlanejamento(planejamentos, transacoes);

    expect(result.categorias).toEqual(['Lazer', 'Mercado', 'Moradia']);
    expect(result.planejadoPorCategoria).toEqual([100, 300, 0]);
    expect(result.executadoPorCategoria).toEqual([0, 250, 500]);
    expect(result.totalPlanejado).toBe(400);
    expect(result.totalExecutado).toBe(750);
  });
});
