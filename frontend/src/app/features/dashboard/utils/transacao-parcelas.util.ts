import { Frequencia, TransacaoRecorrenteResponse } from '../../../core/models/transacao-recorrente.models';
import { TransacaoResponse } from '../../../core/models/transacao.models';

export function buildParcelaInfo(
  tx: TransacaoResponse,
  recorrentes: TransacaoRecorrenteResponse[]
): string {
  if (!tx.transacaoRecorrenteId) {
    return '';
  }

  const recorrente = recorrentes.find((item) => item.id === tx.transacaoRecorrenteId);
  if (!recorrente) {
    return '';
  }

  const total = recorrente.totalParcelas ?? calcularTotalParcelasPorDataFim(recorrente);
  if (!total) {
    return '';
  }

  const numero = calcularNumeroParcela(recorrente, tx.data);
  return numero ? `${numero}/${total}` : '';
}

export function calcularNumeroParcela(
  recorrente: TransacaoRecorrenteResponse,
  data: string
): number | null {
  const inicio = new Date(`${recorrente.dataInicio}T00:00:00`);
  const alvo = new Date(`${data}T00:00:00`);

  if (alvo < inicio) {
    return null;
  }

  const indice = calcularIndiceRecorrencia(inicio, alvo, recorrente.frequencia);
  return indice >= 0 ? indice + 1 : null;
}

export function calcularTotalParcelasPorDataFim(
  recorrente: TransacaoRecorrenteResponse
): number | null {
  if (!recorrente.dataFim) {
    return null;
  }

  const inicio = new Date(`${recorrente.dataInicio}T00:00:00`);
  const fim = new Date(`${recorrente.dataFim}T00:00:00`);

  if (fim < inicio) {
    return null;
  }

  return calcularIndiceRecorrencia(inicio, fim, recorrente.frequencia) + 1;
}

export function calcularIndiceRecorrencia(
  inicio: Date,
  alvo: Date,
  frequencia: Frequencia
): number {
  const msDia = 1000 * 60 * 60 * 24;

  switch (frequencia) {
    case 'SEMANAL':
      return Math.floor((alvo.getTime() - inicio.getTime()) / (msDia * 7));
    case 'MENSAL':
      return (alvo.getFullYear() - inicio.getFullYear()) * 12 + (alvo.getMonth() - inicio.getMonth());
    case 'ANUAL':
      return alvo.getFullYear() - inicio.getFullYear();
    default:
      return 0;
  }
}
