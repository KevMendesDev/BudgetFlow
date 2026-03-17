package br.com.budgetflow.features.categorias.dto;

import br.com.budgetflow.common.enums.ClassificacaoCategoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoriaRequestDTO(
    @NotBlank(message = "O nome da categoria é obrigatório")
    @Size(max = 100, message = "O nome da categoria deve conter no máximo 100 caracteres")
    String nome,

    @NotNull(message = "A classificação da categoria é obrigatória")
    ClassificacaoCategoria classificacao
) {}
