package br.com.budgetflow.features.movimentacoes.dto;

import br.com.budgetflow.common.enums.TipoMovimentacao;
import br.com.budgetflow.common.enums.TipoPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransacaoRequestDTO(
        Long categoriaId,

        @Size(max = 255, message = "A descrição deve conter no máximo 255 caracteres")
        String descricao,

        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
        BigDecimal valor,

        TipoMovimentacao tipoMovimentacao,

        TipoPagamento tipoPagamento,

        Long periodoId,

        Long transacaoRecorrenteId,

        @NotNull(message = "A data é obrigatória")
        LocalDate data
) {}