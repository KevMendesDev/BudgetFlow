package br.com.budgetflow.features.categorias.dto;

import br.com.budgetflow.common.enums.ClassificacaoCategoria;
import br.com.budgetflow.common.enums.NaturezaFinanceira;

public record CategoriaResponseDTO(
    Long id,
    String nome,
    ClassificacaoCategoria classificacao,
    NaturezaFinanceira tipoCategoria,
    String userId,
    String createdAt,
    String updatedAt,
    boolean possuiRelacionamentos
) {
}
