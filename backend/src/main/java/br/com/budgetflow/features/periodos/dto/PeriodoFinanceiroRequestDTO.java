package br.com.budgetflow.features.periodos.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record PeriodoFinanceiroRequestDTO(

    @NotNull(message = "A data de início é obrigatória")
	LocalDate dataInicio,

	@NotNull(message = "A data de fim é obrigatória")
	LocalDate dataFim
) {}