package br.com.budgetflow.features.planejamentos.repository.specification;

import br.com.budgetflow.common.enums.ClassificacaoCategoria;
import br.com.budgetflow.common.enums.NaturezaFinanceira;
import br.com.budgetflow.features.planejamentos.criteria.PlanejamentoFilterCriteria;
import br.com.budgetflow.features.planejamentos.domain.Planejamento;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class PlanejamentoSpecification {

    private PlanejamentoSpecification() {
    }

    public static Specification<Planejamento> createSpecification(PlanejamentoFilterCriteria criteria, Long userId) {
        return Specification
                .where(fetchAssociations())
                .and(hasUserId(userId))
                .and(notExcluido())
                .and(hasPeriodoId(criteria.getPeriodoId()))
                .and(hasTipoMovimentacao(criteria.getTipoMovimentacao()))
                .and(hasClassificacaoCategoria(criteria.getClassificacaoCategoria()))
                .and(hasSearchTerm(criteria.getQuery()));
    }

    private static Specification<Planejamento> fetchAssociations() {
        return (root, query, cb) -> {
            if (!isCountQuery(query)) {
                root.fetch("categoria", JoinType.LEFT);
                root.fetch("periodo", JoinType.LEFT);
                root.fetch("user", JoinType.LEFT);
                query.distinct(true);
            }
            return null;
        };
    }

    private static boolean isCountQuery(jakarta.persistence.criteria.CriteriaQuery<?> query) {
        Class<?> resultType = query.getResultType();
        return Long.class.equals(resultType) || long.class.equals(resultType);
    }

    private static Specification<Planejamento> hasUserId(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    private static Specification<Planejamento> notExcluido() {
        return (root, query, cb) -> cb.isFalse(root.get("excluido"));
    }

    private static Specification<Planejamento> hasPeriodoId(Long periodoId) {
        return (root, query, cb) -> periodoId == null
                ? null
                : cb.equal(root.get("periodo").get("id"), periodoId);
    }

    private static Specification<Planejamento> hasTipoMovimentacao(NaturezaFinanceira tipoMovimentacao) {
        return (root, query, cb) -> tipoMovimentacao == null
                ? null
                : cb.equal(root.get("tipoMovimentacao"), tipoMovimentacao);
    }

    private static Specification<Planejamento> hasClassificacaoCategoria(ClassificacaoCategoria classificacao) {
        return (root, query, cb) -> classificacao == null
                ? null
                : cb.equal(root.join("categoria", JoinType.INNER).get("classificacao"), classificacao);
    }

    private static Specification<Planejamento> hasSearchTerm(String searchTerm) {
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
