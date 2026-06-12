package br.com.budgetflow.features.movimentacoes.dto;

import br.com.budgetflow.common.enums.Frequencia;
import br.com.budgetflow.common.enums.NaturezaFinanceira;
import br.com.budgetflow.common.enums.TipoPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransacaoRecorrenteRequestDTO(
        @NotNull(message = "A categoria é obrigatória")
        Long categoriaId,

        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 255, message = "A descrição deve conter no máximo 255 caracteres")
        String descricao,

        @DecimalMin(value = "0.01", message = "O valor da parcela deve ser maior que zero")
        BigDecimal valorParcela,

        @NotNull(message = "O tipo de movimentação é obrigatório")
        NaturezaFinanceira tipoMovimentacao,

        @NotNull(message = "O tipo de pagamento é obrigatório")
        TipoPagamento tipoPagamento,

        @NotNull(message = "A frequência é obrigatória")
        Frequencia frequencia,

        @NotNull(message = "A data de início é obrigatória")
        LocalDate dataInicio,

        LocalDate dataFim,

        @Positive(message = "O total de parcelas deve ser maior que zero")
        Integer totalParcelas
) {
}
