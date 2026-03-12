package br.com.budgetflow.features.auth.dto;

import java.util.List;

public record MeResponse(
        Long id,
        String nome,
        String email,
        String cpf,
        List<String> roles
) {}
