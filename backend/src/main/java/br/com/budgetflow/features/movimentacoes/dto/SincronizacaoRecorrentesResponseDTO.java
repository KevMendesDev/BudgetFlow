package br.com.budgetflow.features.movimentacoes.dto;

import java.util.List;

public record SincronizacaoRecorrentesResponseDTO(
        int transacoesGeradas,
        int transacoesRecorrentesSemValor,
        List<String> transacoesRecorrentesPendentes,
        String mensagem
) {
}
