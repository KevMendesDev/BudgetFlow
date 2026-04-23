export type TipoMovimentacao = 'RECEITA' | 'DESPESA';
export type TipoPagamento = 'DINHEIRO' | 'CARTAO_CREDITO' | 'CARTAO_DEBITO' | 'PIX' | 'TRANSFERENCIA' | 'BOLETO';

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

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
