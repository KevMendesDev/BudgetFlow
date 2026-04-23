import { PageResponse } from './transacao.models';

export type ClassificacaoCategoria = 'ESSENCIAL' | 'NAO_ESSENCIAL' | 'INVESTIMENTO';

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
