import {
  buildDailyChartData,
  buildResumoCategorias,
  buildResumoClassificacao,
  filterDespesas,
  sumValores,
} from './dashboard-resumo.util';
import { TransacaoResponse } from '../../../core/models/transacao.models';

describe('dashboard-resumo.util', () => {
  const transacoes = [
    {
      categoriaNome: 'Moradia',
      classificacaoCategoria: 'ESSENCIAL',
      tipoMovimentacao: 'DESPESA',
      valor: 100,
      data: '2026-01-01',
    },
    {
      categoriaNome: 'Mercado',
      classificacaoCategoria: 'ESSENCIAL',
      tipoMovimentacao: 'DESPESA',
      valor: 50,
      data: '2026-01-02',
    },
    {
      categoriaNome: 'Salário',
      classificacaoCategoria: null,
      tipoMovimentacao: 'RECEITA',
      valor: 500,
      data: '2026-01-02',
    },
  ] as TransacaoResponse[];

  it('agrega categorias e classificações', () => {
    const despesas = filterDespesas(transacoes);
    const receitas = sumValores(transacoes.filter((tx) => tx.tipoMovimentacao === 'RECEITA'));
    const saldo = receitas - sumValores(despesas);

    const resumoCategorias = buildResumoCategorias(despesas, saldo, receitas, (key) => key);
    const resumoClassificacao = buildResumoClassificacao(despesas);

    expect(resumoCategorias.map((item) => item.nome)).toEqual(['Moradia', 'Mercado']);
    expect(resumoClassificacao[0]).toMatchObject({ classificacao: 'ESSENCIAL', total: 150 });
  });

  it('monta dados do gráfico diário', () => {
    const chart = buildDailyChartData(
      {
        dataInicio: '2026-01-01',
        dataFim: '2026-01-02',
      } as never,
      filterDespesas(transacoes),
      (key) => key
    );

    expect(chart.labels).toEqual(['01/01/2026', '02/01/2026']);
    expect(chart.datasets).toHaveLength(2);
  });
});
