package br.com.budgetflow.features.movimentacoes.dto;

import br.com.budgetflow.common.enums.ClassificacaoCategoria;
import br.com.budgetflow.common.enums.TipoMovimentacao;
import br.com.budgetflow.common.enums.TipoPagamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransacaoResponseDTO(
        Long id,
        Long userId,
        Long categoriaId,
        String categoriaNome,
        ClassificacaoCategoria classificacaoCategoria,
        Long periodoId,
        Long transacaoRecorrenteId,
        String descricao,
        BigDecimal valor,
        TipoMovimentacao tipoMovimentacao,
        TipoPagamento tipoPagamento,
        LocalDate data,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
