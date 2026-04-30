import { PageResponse } from './pagination.models';

export type TipoMovimentacao = 'RECEITA' | 'DESPESA';
export type TipoPagamento =
  | 'DINHEIRO'
  | 'CARTAO_CREDITO'
  | 'CARTAO_DEBITO'
  | 'PIX'
  | 'TRANSFERENCIA'
  | 'BOLETO';

export const TIPOS_MOVIMENTACAO: Array<{ value: TipoMovimentacao; label: string }> = [
  { value: 'RECEITA', label: 'Receita' },
  { value: 'DESPESA', label: 'Despesa' },
];

export const TIPOS_PAGAMENTO: Array<{ value: TipoPagamento; label: string }> = [
  { value: 'DINHEIRO', label: 'Dinheiro' },
  { value: 'CARTAO_CREDITO', label: 'Cartão de crédito' },
  { value: 'CARTAO_DEBITO', label: 'Cartão de débito' },
  { value: 'PIX', label: 'Pix' },
  { value: 'TRANSFERENCIA', label: 'Transferência' },
  { value: 'BOLETO', label: 'Boleto' },
];

export const TIPO_MOVIMENTACAO_LABELS: Record<TipoMovimentacao, string> = {
  RECEITA: 'Receita',
  DESPESA: 'Despesa',
};

export const TIPO_PAGAMENTO_LABELS: Record<TipoPagamento, string> = {
  DINHEIRO: 'Dinheiro',
  CARTAO_CREDITO: 'Cartão de crédito',
  CARTAO_DEBITO: 'Cartão de débito',
  PIX: 'Pix',
  TRANSFERENCIA: 'Transferência',
  BOLETO: 'Boleto',
};

export interface TransacaoResponse {
  id: number;
  userId: number;
  categoriaId: number;
  categoriaNome: string;
  classificacaoCategoria: string;
  periodoId: number;
  transacaoRecorrenteId: number | null;
  descricao: string;
  valor: number;
  tipoMovimentacao: TipoMovimentacao;
  tipoPagamento: TipoPagamento;
  data: string;
  createdAt: string;
  updatedAt: string;
}

export interface TransacaoRequest {
  categoriaId: number | null;
  descricao: string | null;
  valor: number | null;
  tipoMovimentacao: TipoMovimentacao | null;
  tipoPagamento: TipoPagamento | null;
  periodoId: number;
  transacaoRecorrenteId: number | null;
  data: string;
}

export type { PageResponse };
