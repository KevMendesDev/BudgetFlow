import { ClassificacaoCategoria } from './categoria.models';
import { PageResponse } from './transacao.models';

export type Frequencia = 'DIARIO' | 'SEMANAL' | 'MENSAL' | 'ANUAL';

export type TipoMovimentacao = 'RECEITA' | 'DESPESA';

export type TipoPagamento = 'DINHEIRO' | 'CARTAO_CREDITO' | 'CARTAO_DEBITO' | 'PIX' | 'TRANSFERENCIA' | 'BOLETO';

export interface TransacaoRecorrenteRequest {
  categoriaId: number;
  descricao: string;
  valorParcela: number;
  tipoMovimentacao: TipoMovimentacao;
  tipoPagamento: TipoPagamento;
  frequencia: Frequencia;
  dataInicio: string;
  dataFim: string | null;
  totalParcelas: number | null;
}

export interface TransacaoRecorrenteResponse {
  id: number;
  userId: number;
  categoriaId: number;
  categoriaNome: string;
  classificacaoCategoria: ClassificacaoCategoria;
  descricao: string;
  valorParcela: number;
  valorTotal: number;
  tipoMovimentacao: TipoMovimentacao;
  tipoPagamento: TipoPagamento;
  frequencia: Frequencia;
  dataInicio: string;
  dataFim: string | null;
  totalParcelas: number | null;
  createdAt: string;
  updatedAt: string;
}

export type TransacaoRecorrentePageResponse = PageResponse<TransacaoRecorrenteResponse>;
