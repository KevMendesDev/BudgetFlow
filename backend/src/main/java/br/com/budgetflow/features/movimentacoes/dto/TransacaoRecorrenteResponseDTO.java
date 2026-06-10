package br.com.budgetflow.features.movimentacoes.dto;

import br.com.budgetflow.common.enums.ClassificacaoCategoria;
import br.com.budgetflow.common.enums.Frequencia;
import br.com.budgetflow.common.enums.NaturezaFinanceira;
import br.com.budgetflow.common.enums.TipoPagamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransacaoRecorrenteResponseDTO(
        Long id,
        Long userId,
        Long categoriaId,
        String categoriaNome,
        ClassificacaoCategoria classificacaoCategoria,
        String descricao,
        BigDecimal valorParcela,
        BigDecimal valorTotal,
        NaturezaFinanceira tipoMovimentacao,
        TipoPagamento tipoPagamento,
        Frequencia frequencia,
        LocalDate dataInicio,
        LocalDate dataFim,
        Integer totalParcelas,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean possuiRelacionamentos
) {
}
