package br.com.budgetflow.features.categorias.dto;

import br.com.budgetflow.common.enums.ClassificacaoCategoria;

public record CategoriaResponseDTO(
    Long id,
    String nome,
    ClassificacaoCategoria classificacao,
    String userId,
    String createdAt,
    String updatedAt,
    boolean possuiRelacionamentos
) {
}
