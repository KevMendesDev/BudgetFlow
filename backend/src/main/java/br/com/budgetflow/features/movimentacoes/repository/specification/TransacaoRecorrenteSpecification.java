package br.com.budgetflow.features.movimentacoes.repository.specification;

import br.com.budgetflow.common.enums.ClassificacaoCategoria;
import br.com.budgetflow.common.enums.Frequencia;
import br.com.budgetflow.common.enums.TipoMovimentacao;
import br.com.budgetflow.common.enums.TipoPagamento;
import br.com.budgetflow.features.movimentacoes.criteria.TransacaoRecorrenteFilterCriteria;
import br.com.budgetflow.features.movimentacoes.domain.TransacaoRecorrente;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class TransacaoRecorrenteSpecification {

    private TransacaoRecorrenteSpecification() {
    }

    public static Specification<TransacaoRecorrente> createSpecification(TransacaoRecorrenteFilterCriteria criteria, Long userId) {
        Specification<TransacaoRecorrente> specification = Specification
            .where(TransacaoRecorrenteSpecification.hasUserId(userId))
            .and(TransacaoRecorrenteSpecification.hasDataInicioFrom(criteria.getDataInicio()))
            .and(TransacaoRecorrenteSpecification.hasDataInicioTo(criteria.getDataFim()))
            .and(TransacaoRecorrenteSpecification.hasFrequencia(criteria.getFrequencia()))
            .and(TransacaoRecorrenteSpecification.hasTotalParcelas(criteria.getTotalParcelas()))
            .and(TransacaoRecorrenteSpecification.hasValorMin(criteria.getValorMin()))
            .and(TransacaoRecorrenteSpecification.hasValorMax(criteria.getValorMax()))
            .and(TransacaoRecorrenteSpecification.hasNomeCategoria(criteria.getNomeCategoria()))
            .and(TransacaoRecorrenteSpecification.hasClassificacaoCategoria(criteria.getClassificacaoCategoria()))
            .and(TransacaoRecorrenteSpecification.hasTipoMovimentacao(criteria.getTipoMovimentacao()))
            .and(TransacaoRecorrenteSpecification.hasTipoPagamento(criteria.getTipoPagamento()))
            .and(TransacaoRecorrenteSpecification.hasSearchTerm(criteria.getQuery()));
        return specification;
    }

    private static Specification<TransacaoRecorrente> hasUserId(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    private static Specification<TransacaoRecorrente> hasDataInicioFrom(LocalDate dataInicio) {
        return (root, query, cb) -> dataInicio == null
                ? null
                : cb.greaterThanOrEqualTo(root.get("dataInicio"), dataInicio);
    }

    private static Specification<TransacaoRecorrente> hasDataInicioTo(LocalDate dataFim) {
        return (root, query, cb) -> dataFim == null
                ? null
                : cb.lessThanOrEqualTo(root.get("dataInicio"), dataFim);
    }

    private static Specification<TransacaoRecorrente> hasFrequencia(Frequencia frequencia) {
        return (root, query, cb) -> frequencia == null
                ? null
                : cb.equal(root.get("frequencia"), frequencia);
    }

    private static Specification<TransacaoRecorrente> hasTotalParcelas(Integer totalParcelas) {
        return (root, query, cb) -> totalParcelas == null
                ? null
                : cb.equal(root.get("totalParcelas"), totalParcelas);
    }

    private static Specification<TransacaoRecorrente> hasValorMin(BigDecimal valorMin) {
        return (root, query, cb) -> valorMin == null
                ? null
                : cb.greaterThanOrEqualTo(root.get("valor"), valorMin);
    }

    private static Specification<TransacaoRecorrente> hasValorMax(BigDecimal valorMax) {
        return (root, query, cb) -> valorMax == null
                ? null
                : cb.lessThanOrEqualTo(root.get("valor"), valorMax);
    }

    private static Specification<TransacaoRecorrente> hasNomeCategoria(String nomeCategoria) {
        return (root, query, cb) -> {
            if (nomeCategoria == null || nomeCategoria.isBlank()) {
                return null;
            }
            String normalized = "%" + nomeCategoria.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.join("categoria", JoinType.INNER).get("nome")), normalized);
        };
    }

    private static Specification<TransacaoRecorrente> hasClassificacaoCategoria(ClassificacaoCategoria classificacao) {
        return (root, query, cb) -> classificacao == null
                ? null
                : cb.equal(root.join("categoria", JoinType.INNER).get("classificacao"), classificacao);
    }

    private static Specification<TransacaoRecorrente> hasTipoMovimentacao(TipoMovimentacao tipoMovimentacao) {
        return (root, query, cb) -> tipoMovimentacao == null
                ? null
                : cb.equal(root.get("tipoMovimentacao"), tipoMovimentacao);
    }

    private static Specification<TransacaoRecorrente> hasTipoPagamento(TipoPagamento tipoPagamento) {
        return (root, query, cb) -> tipoPagamento == null
                ? null
                : cb.equal(root.get("tipoPagamento"), tipoPagamento);
    }

    private static Specification<TransacaoRecorrente> hasSearchTerm(String searchTerm) {
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
