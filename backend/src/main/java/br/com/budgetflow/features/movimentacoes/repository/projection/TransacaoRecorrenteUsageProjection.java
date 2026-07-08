package br.com.budgetflow.features.movimentacoes.repository.projection;

import java.time.LocalDate;

import br.com.budgetflow.features.movimentacoes.domain.TransacaoRecorrente;

public interface TransacaoRecorrenteUsageProjection {
    TransacaoRecorrente getRecorrente();
    long getParcelasLancadas();
    LocalDate getUltimaData();
}
