package br.com.budgetflow.features.periodos.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PeriodoFinanceiroResponseDTO(
    Long id,
    Long userId,
    LocalDate dataInicio,
    LocalDate dataFim,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    boolean possuiRelacionamentos
) {}
