package br.com.budgetflow.features.auth.dto;

public record CsrfTokenResponseDTO(
        String token,
        String headerName
) {
}
