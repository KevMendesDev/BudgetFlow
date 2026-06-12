package br.com.budgetflow.common.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import br.com.budgetflow.features.movimentacoes.repository.TransacaoRecorrenteRepository;
import br.com.budgetflow.features.movimentacoes.repository.TransacaoRepository;

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

    public Set<Long> findCategoriaIdsWithRelationships(Collection<Long> categoriaIds, Long userId) {
        Set<Long> relatedIds = new HashSet<>();
        relatedIds.addAll(transacaoRepository.findCategoriaIdsByUserIdAndCategoriaIds(userId, categoriaIds));
        relatedIds.addAll(transacaoRecorrenteRepository.findCategoriaIdsByUserIdAndCategoriaIds(userId, categoriaIds));
        return relatedIds;
    }

    public Set<Long> findPeriodoIdsWithRelationships(Collection<Long> periodoIds, Long userId) {
        return new HashSet<>(transacaoRepository.findPeriodoIdsByUserIdAndPeriodoIds(userId, periodoIds));
    }

    public boolean periodoHasRelationships(Long periodoId, Long userId) {
        return transacaoRepository.existsByPeriodoIdAndUserId(periodoId, userId);
    }

    public Set<Long> findTransacaoRecorrenteIdsWithRelationships(Collection<Long> transacaoRecorrenteIds, Long userId) {
        return new HashSet<>(
                transacaoRepository.findTransacaoRecorrenteIdsByUserIdAndTransacaoRecorrenteIds(userId, transacaoRecorrenteIds)
        );
    }

    public boolean transacaoRecorrenteHasRelationships(Long transacaoRecorrenteId, Long userId) {
        return transacaoRepository.existsByTransacaoRecorrenteIdAndUserId(transacaoRecorrenteId, userId);
    }
}
