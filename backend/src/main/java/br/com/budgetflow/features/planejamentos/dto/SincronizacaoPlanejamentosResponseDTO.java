package br.com.budgetflow.features.planejamentos.dto;

public record SincronizacaoPlanejamentosResponseDTO(
        int planejamentosGerados,
        int recorrenciasSemValor,
        String mensagem
) {
}
