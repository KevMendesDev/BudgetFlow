import { PageResponse } from './pagination.models';

export type ClassificacaoCategoria = 'ESSENCIAL' | 'NAO_ESSENCIAL' | 'INVESTIMENTO';

export const CLASSIFICACOES: Array<{ value: ClassificacaoCategoria; label: string }> = [
  { value: 'ESSENCIAL', label: 'Essencial' },
  { value: 'NAO_ESSENCIAL', label: 'Não essencial' },
  { value: 'INVESTIMENTO', label: 'Investimento' },
];

export const CLASSIFICACAO_LABELS: Record<ClassificacaoCategoria, string> = {
  ESSENCIAL: 'Essencial',
  NAO_ESSENCIAL: 'Não essencial',
  INVESTIMENTO: 'Investimento',
};

export interface CategoriaRequest {
  nome: string;
  classificacao: ClassificacaoCategoria;
}

export interface CategoriaResponse {
  id: number;
  nome: string;
  classificacao: ClassificacaoCategoria;
  userId: string;
  createdAt: string;
  updatedAt: string;
}

export type CategoriaPageResponse = PageResponse<CategoriaResponse>;
