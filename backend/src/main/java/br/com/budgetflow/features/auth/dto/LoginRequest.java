package br.com.budgetflow.features.auth.dto;

import br.com.budgetflow.common.validation.Cpf;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "CPF é obrigatório")
        @Cpf
        String cpf,

        @NotBlank(message = "Senha é obrigatória")
        String senha
) {}
