import { PageResponse } from './pagination.models';

export type ClassificacaoCategoria = 'ESSENCIAL' | 'NAO_ESSENCIAL' | 'INVESTIMENTO';

export const CLASSIFICACOES: Array<{ value: ClassificacaoCategoria; label: string }> = [
  { value: 'ESSENCIAL', label: 'Essencial' },
  { value: 'NAO_ESSENCIAL', label: 'Não essencial' },
  { value: 'INVESTIMENTO', label: 'Investimento' },
];

export const CLASSIFICACAO_LABELS = Object.fromEntries(
  CLASSIFICACOES.map(({ value, label }) => [value, label])
) as Record<ClassificacaoCategoria, string>;

export interface CategoriaRequest {
  nome: string;
  classificacao: ClassificacaoCategoria;
}

export interface CategoriaResponse {
  id: number;
  nome: string;
  classificacao: ClassificacaoCategoria;
  userId: number;
  createdAt: string;
  updatedAt: string;
}

export type CategoriaPageResponse = PageResponse<CategoriaResponse>;
