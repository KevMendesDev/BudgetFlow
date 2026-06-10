package br.com.budgetflow.features.movimentacoes.repository.specification;

import br.com.budgetflow.common.enums.ClassificacaoCategoria;
import br.com.budgetflow.common.enums.NaturezaFinanceira;
import br.com.budgetflow.common.enums.TipoPagamento;
import br.com.budgetflow.features.movimentacoes.criteria.TransacaoFilterCriteria;
import br.com.budgetflow.features.movimentacoes.domain.Transacao;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class TransacaoSpecification {

    private TransacaoSpecification() {
    }

    public static Specification<Transacao> createSpecification (TransacaoFilterCriteria criteria, Long userId, Long effectivePeriodoId) {
        Specification<Transacao> specification = Specification
            .where(TransacaoSpecification.hasUserId(userId))
            .and(TransacaoSpecification.hasDataFrom(criteria.getDataInicio()))
            .and(TransacaoSpecification.hasDataTo(criteria.getDataFim()))
            .and(TransacaoSpecification.hasPeriodoId(effectivePeriodoId))
            .and(TransacaoSpecification.hasRecorrente(criteria.getRecorrente()))
            .and(TransacaoSpecification.hasValorMin(criteria.getValorMin()))
            .and(TransacaoSpecification.hasValorMax(criteria.getValorMax()))
            .and(TransacaoSpecification.hasNomeCategoria(criteria.getNomeCategoria()))
            .and(TransacaoSpecification.hasClassificacaoCategoria(criteria.getClassificacaoCategoria()))
            .and(TransacaoSpecification.hasTipoMovimentacao(criteria.getTipoMovimentacao()))
            .and(TransacaoSpecification.hasTipoPagamento(criteria.getTipoPagamento()))
            .and(TransacaoSpecification.hasSearchTerm(criteria.getQuery()));

        return specification;
    }

    private static Specification<Transacao> hasUserId(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    private static Specification<Transacao> hasDataFrom(LocalDate dataInicio) {
        return (root, query, cb) -> dataInicio == null
                ? null
                : cb.greaterThanOrEqualTo(root.get("data"), dataInicio);
    }

    private static Specification<Transacao> hasDataTo(LocalDate dataFim) {
        return (root, query, cb) -> dataFim == null
                ? null
                : cb.lessThanOrEqualTo(root.get("data"), dataFim);
    }

    private static Specification<Transacao> hasPeriodoId(Long periodoId) {
        return (root, query, cb) -> periodoId == null
                ? null
                : cb.equal(root.get("periodo").get("id"), periodoId);
    }

    private static Specification<Transacao> hasRecorrente(Boolean recorrente) {
        return (root, query, cb) -> {
            if (recorrente == null) {
                return null;
            }
            return recorrente
                    ? cb.isNotNull(root.get("transacaoRecorrente"))
                    : cb.isNull(root.get("transacaoRecorrente"));
        };
    }

    private static Specification<Transacao> hasValorMin(BigDecimal valorMin) {
        return (root, query, cb) -> valorMin == null
                ? null
                : cb.greaterThanOrEqualTo(root.get("valor"), valorMin);
    }

    private static Specification<Transacao> hasValorMax(BigDecimal valorMax) {
        return (root, query, cb) -> valorMax == null
                ? null
                : cb.lessThanOrEqualTo(root.get("valor"), valorMax);
    }

    private static Specification<Transacao> hasNomeCategoria(String nomeCategoria) {
        return (root, query, cb) -> {
            if (nomeCategoria == null || nomeCategoria.isBlank()) {
                return null;
            }
            String normalized = "%" + nomeCategoria.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.join("categoria", JoinType.INNER).get("nome")), normalized);
        };
    }

    private static Specification<Transacao> hasClassificacaoCategoria(ClassificacaoCategoria classificacao) {
        return (root, query, cb) -> classificacao == null
                ? null
                : cb.equal(root.join("categoria", JoinType.INNER).get("classificacao"), classificacao);
    }

    private static Specification<Transacao> hasTipoMovimentacao(NaturezaFinanceira tipoMovimentacao) {
        return (root, query, cb) -> tipoMovimentacao == null
                ? null
                : cb.equal(root.get("tipoMovimentacao"), tipoMovimentacao);
    }

    private static Specification<Transacao> hasTipoPagamento(TipoPagamento tipoPagamento) {
        return (root, query, cb) -> tipoPagamento == null
                ? null
                : cb.equal(root.get("tipoPagamento"), tipoPagamento);
    }

    private static Specification<Transacao> hasSearchTerm(String searchTerm) {
        return (root, query, cb) -> {
            if (searchTerm == null || searchTerm.isBlank()) {
                return null;
            }
            String normalized = "%" + searchTerm.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("descricao")), normalized),
                    cb.like(cb.lower(root.join("categoria", JoinType.INNER).get("nome")), normalized)
            );
        };
    }
}
