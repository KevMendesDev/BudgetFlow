package br.com.budgetflow.features.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank(message = "CPF é obrigatório")
        String cpf,

        @NotBlank(message = "Senha é obrigatória")
        String senha
) {}
