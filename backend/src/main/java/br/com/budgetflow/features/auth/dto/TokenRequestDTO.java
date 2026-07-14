package br.com.budgetflow.features.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRequestDTO(
        @NotBlank(message = "Token é obrigatório")
        String token
) {
}
