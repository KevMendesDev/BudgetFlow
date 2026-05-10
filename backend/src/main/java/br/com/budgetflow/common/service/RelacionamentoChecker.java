package br.com.budgetflow.common.service;

import br.com.budgetflow.features.movimentacoes.repository.TransacaoRecorrenteRepository;
import br.com.budgetflow.features.movimentacoes.repository.TransacaoRepository;
import org.springframework.stereotype.Component;

@Component
public class RelacionamentoChecker {

    private final TransacaoRepository transacaoRepository;
    private final TransacaoRecorrenteRepository transacaoRecorrenteRepository;

    public RelacionamentoChecker(
            TransacaoRepository transacaoRepository,
            TransacaoRecorrenteRepository transacaoRecorrenteRepository
    ) {
        this.transacaoRepository = transacaoRepository;
        this.transacaoRecorrenteRepository = transacaoRecorrenteRepository;
    }

    public boolean categoriaHasRelationships(Long categoriaId, Long userId) {
        return transacaoRepository.existsByCategoriaIdAndUserId(categoriaId, userId)
                || transacaoRecorrenteRepository.existsByCategoriaIdAndUserId(categoriaId, userId);
    }

    public boolean periodoHasRelationships(Long periodoId, Long userId) {
        return transacaoRepository.existsByPeriodoIdAndUserId(periodoId, userId);
    }

    public boolean transacaoRecorrenteHasRelationships(Long transacaoRecorrenteId, Long userId) {
        return transacaoRepository.existsByTransacaoRecorrenteIdAndUserId(transacaoRecorrenteId, userId);
    }
}
