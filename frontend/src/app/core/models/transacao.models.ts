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

export const TIPO_MOVIMENTACAO_LABELS = Object.fromEntries(
  TIPOS_MOVIMENTACAO.map(({ value, label }) => [value, label])
) as Record<TipoMovimentacao, string>;

export const TIPO_PAGAMENTO_LABELS = Object.fromEntries(
  TIPOS_PAGAMENTO.map(({ value, label }) => [value, label])
) as Record<TipoPagamento, string>;

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
