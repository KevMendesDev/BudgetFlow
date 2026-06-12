package br.com.budgetflow.features.periodos.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PeriodoFinanceiroRequestDTO(
    @NotNull(message = "O mês é obrigatório")
    @Min(value = 1, message = "O mês deve estar entre 1 e 12")
    @Max(value = 12, message = "O mês deve estar entre 1 e 12")
    Integer mes,

    @NotNull(message = "O ano é obrigatório")
    @Min(value = 2000, message = "O ano informado é inválido")
    @Max(value = 2100, message = "O ano informado é inválido")
    Integer ano
) {}
