package br.com.budgetflow.common.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import br.com.budgetflow.features.movimentacoes.repository.TransacaoRecorrenteRepository;
import br.com.budgetflow.features.movimentacoes.repository.TransacaoRepository;
import br.com.budgetflow.features.planejamentos.repository.PlanejamentoRepository;

@Component
public class RelacionamentoChecker {

    private final TransacaoRepository transacaoRepository;
    private final TransacaoRecorrenteRepository transacaoRecorrenteRepository;
    private final PlanejamentoRepository planejamentoRepository;

    public RelacionamentoChecker(
            TransacaoRepository transacaoRepository,
            TransacaoRecorrenteRepository transacaoRecorrenteRepository,
            PlanejamentoRepository planejamentoRepository
    ) {
        this.transacaoRepository = transacaoRepository;
        this.transacaoRecorrenteRepository = transacaoRecorrenteRepository;
        this.planejamentoRepository = planejamentoRepository;
    }

    public boolean categoriaHasRelationships(Long categoriaId, Long userId) {
        return transacaoRepository.existsByCategoriaIdAndUserId(categoriaId, userId)
                || transacaoRecorrenteRepository.existsByCategoriaIdAndUserId(categoriaId, userId)
                || planejamentoRepository.existsByCategoriaIdAndUserIdAndExcluidoFalse(categoriaId, userId);
    }

    public Set<Long> findCategoriaIdsWithRelationships(Collection<Long> categoriaIds, Long userId) {
        Set<Long> relatedIds = new HashSet<>();
        relatedIds.addAll(transacaoRepository.findCategoriaIdsByUserIdAndCategoriaIds(userId, categoriaIds));
        relatedIds.addAll(transacaoRecorrenteRepository.findCategoriaIdsByUserIdAndCategoriaIds(userId, categoriaIds));
        relatedIds.addAll(planejamentoRepository.findCategoriaIdsByUserIdAndCategoriaIds(userId, categoriaIds));
        return relatedIds;
    }

    public Set<Long> findPeriodoIdsWithRelationships(Collection<Long> periodoIds, Long userId) {
        Set<Long> relatedIds = new HashSet<>(transacaoRepository.findPeriodoIdsByUserIdAndPeriodoIds(userId, periodoIds));
        relatedIds.addAll(planejamentoRepository.findPeriodoIdsByUserIdAndPeriodoIds(userId, periodoIds));
        return relatedIds;
    }

    public boolean periodoHasRelationships(Long periodoId, Long userId) {
        return transacaoRepository.existsByPeriodoIdAndUserId(periodoId, userId)
                || planejamentoRepository.existsByPeriodoIdAndUserIdAndExcluidoFalse(periodoId, userId);
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
