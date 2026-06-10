import { PageResponse } from './pagination.models';
import { NaturezaFinanceira } from './natureza-financeira.models';

export type ClassificacaoCategoria = 'ESSENCIAL' | 'NAO_ESSENCIAL' | 'INVESTIMENTO';

export const CLASSIFICACOES: Array<{ value: ClassificacaoCategoria; label: string }> = [
  { value: 'ESSENCIAL', label: 'Essencial' },
  { value: 'NAO_ESSENCIAL', label: 'Não essencial' },
  { value: 'INVESTIMENTO', label: 'Investimento' },
];

export const TIPOS_CATEGORIA: Array<{ value: NaturezaFinanceira; label: string }> = [
  { value: NaturezaFinanceira.DESPESA, label: 'Despesa' },
  { value: NaturezaFinanceira.RECEITA, label: 'Receita' },
];

export const CLASSIFICACAO_LABELS = Object.fromEntries(
  CLASSIFICACOES.map(({ value, label }) => [value, label])
) as Record<ClassificacaoCategoria, string>;

export const TIPO_CATEGORIA_LABELS = Object.fromEntries(
  TIPOS_CATEGORIA.map(({ value, label }) => [value, label])
) as Record<NaturezaFinanceira, string>;

export interface CategoriaRequest {
  nome: string;
  classificacao: ClassificacaoCategoria | null;
  tipoCategoria: NaturezaFinanceira;
}

export interface CategoriaResponse {
  id: number;
  nome: string;
  classificacao: ClassificacaoCategoria | null;
  tipoCategoria: NaturezaFinanceira;
  userId: number;
  createdAt: string;
  updatedAt: string;
  possuiRelacionamentos: boolean;
}

export type CategoriaPageResponse = PageResponse<CategoriaResponse>;
