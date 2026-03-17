package br.com.budgetflow.features.categorias.dto;

import br.com.budgetflow.common.enums.ClassificacaoCategoria;
import br.com.budgetflow.features.users.domain.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;

public record CategoriaRequestDTO(
    @NotBlank(message = "O nome da categoria é obrigatório")
    @Max(value = 100, message = "O nome da categoria deve conter no máximo 100 caracteres")
    String nome,

    @NotBlank(message = "A classificação da categoria é obrigatória")
    ClassificacaoCategoria classificacao,
        
    @Valid
    User user
) {}
