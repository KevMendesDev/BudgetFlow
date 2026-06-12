import { buildParcelaInfo } from './transacao-parcelas.util';

describe('transacao-parcelas.util', () => {
  it('calcula número e total da parcela mensal', () => {
    const info = buildParcelaInfo(
      {
        data: '2026-03-10',
        transacaoRecorrenteId: 1,
      } as never,
      [
        {
          id: 1,
          dataInicio: '2026-01-10',
          dataFim: '2026-04-10',
          frequencia: 'MENSAL',
          totalParcelas: null,
        } as never,
      ]
    );

    expect(info).toBe('3/4');
  });
});
