package br.com.budgetflow.features.movimentacoes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.budgetflow.common.enums.NaturezaFinanceira;
import br.com.budgetflow.common.enums.StatusTransacao;
import br.com.budgetflow.common.enums.TipoPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransacaoRequestDTO(
        Long categoriaId,

        @Size(max = 255, message = "A descrição deve conter no máximo 255 caracteres")
        String descricao,

        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
        BigDecimal valor,

        @NotNull(message = "O tipo da movimentação é obrigatório")
        NaturezaFinanceira tipoMovimentacao,

        TipoPagamento tipoPagamento,

        @NotNull(message = "O período da transação é obrigatório")
        Long periodoId,

        Long transacaoRecorrenteId,

        @NotNull(message = "O status da transação é obrigatório")
        StatusTransacao status,

        @NotNull(message = "A data é obrigatória")
        LocalDate data
) {}