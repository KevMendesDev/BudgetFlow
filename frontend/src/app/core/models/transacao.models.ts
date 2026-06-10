import { ClassificacaoCategoria } from './categoria.models';
import { NaturezaFinanceira } from './natureza-financeira.models';

export type TipoPagamento =
  | 'DINHEIRO'
  | 'CARTAO_CREDITO'
  | 'CARTAO_DEBITO'
  | 'PIX'
  | 'TRANSFERENCIA'
  | 'BOLETO';

export const TIPOS_MOVIMENTACAO: Array<{ value: NaturezaFinanceira; label: string }> = [
  { value: NaturezaFinanceira.RECEITA, label: 'Receita' },
  { value: NaturezaFinanceira.DESPESA, label: 'Despesa' },
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
) as Record<NaturezaFinanceira, string>;

export const TIPO_PAGAMENTO_LABELS = Object.fromEntries(
  TIPOS_PAGAMENTO.map(({ value, label }) => [value, label])
) as Record<TipoPagamento, string>;

export interface TransacaoResponse {
  id: number;
  userId: number;
  categoriaId: number;
  categoriaNome: string;
  classificacaoCategoria: ClassificacaoCategoria | null;
  periodoId: number;
  transacaoRecorrenteId: number | null;
  descricao: string;
  valor: number;
  tipoMovimentacao: NaturezaFinanceira;
  tipoPagamento: TipoPagamento;
  data: string;
  createdAt: string;
  updatedAt: string;
}

export interface TransacaoRequest {
  categoriaId: number | null;
  descricao: string | null;
  valor: number | null;
  tipoMovimentacao: NaturezaFinanceira | null;
  tipoPagamento: TipoPagamento | null;
  periodoId: number;
  transacaoRecorrenteId: number | null;
  data: string;
}
