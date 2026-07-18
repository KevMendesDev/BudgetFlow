package br.com.budgetflow.features.planejamentos.dto;

import java.math.BigDecimal;

import br.com.budgetflow.common.enums.NaturezaFinanceira;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlanejamentoRequestDTO(
        @NotNull(message = "A categoria é obrigatória")
        Long categoriaId,

        @NotNull(message = "O período é obrigatório")
        Long periodoId,

        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 255, message = "A descrição deve conter no máximo 255 caracteres")
        String descricao,

        @NotNull(message = "O valor é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
        BigDecimal valor,

        @NotNull(message = "O tipo da movimentação é obrigatório")
        NaturezaFinanceira tipoMovimentacao,

        Long transacaoRecorrenteId
) {
}
