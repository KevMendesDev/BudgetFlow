package br.com.budgetflow.features.auth.dto;

import br.com.budgetflow.common.validation.Cpf;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequestDTO(
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        @NotBlank(message = "CPF é obrigatório")
        @Cpf
        String cpf,

        String telefone,

        @NotBlank(message = "Senha é obrigatória")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).{8,}$",
                message = "Senha deve ter no mínimo 8 caracteres e conter letra maiúscula, minúscula, número e caractere especial"
        )
        String senha
) {}
