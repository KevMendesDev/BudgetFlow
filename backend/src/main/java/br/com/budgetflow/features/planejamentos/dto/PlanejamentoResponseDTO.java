package br.com.budgetflow.features.planejamentos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.com.budgetflow.common.enums.ClassificacaoCategoria;
import br.com.budgetflow.common.enums.NaturezaFinanceira;

public record PlanejamentoResponseDTO(
        Long id,
        Long userId,
        Long categoriaId,
        String categoriaNome,
        ClassificacaoCategoria classificacaoCategoria,
        Long periodoId,
        String descricao,
        BigDecimal valor,
        NaturezaFinanceira tipoMovimentacao,
        boolean sincronizado,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
