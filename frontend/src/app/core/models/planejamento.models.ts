import { ClassificacaoCategoria } from './categoria.models';
import { NaturezaFinanceira } from './natureza-financeira.models';
import { PageResponse } from './pagination.models';

export interface PlanejamentoResponse {
  id: number;
  userId: number;
  categoriaId: number;
  categoriaNome: string;
  classificacaoCategoria: ClassificacaoCategoria | null;
  periodoId: number;
  descricao: string;
  valor: number;
  tipoMovimentacao: NaturezaFinanceira;
  sincronizado: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PlanejamentoRequest {
  categoriaId: number;
  periodoId: number;
  descricao: string;
  valor: number;
  tipoMovimentacao: NaturezaFinanceira;
  transacaoRecorrenteId?: number | null;
}

export interface SincronizacaoPlanejamentosResponse {
  planejamentosGerados: number;
  recorrenciasSemValor: number;
  mensagem: string;
}

export type PlanejamentoPageResponse = PageResponse<PlanejamentoResponse>;
