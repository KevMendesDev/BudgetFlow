package br.com.budgetflow.features.periodos.repository.specification;

import br.com.budgetflow.features.periodos.domain.PeriodoFinanceiro;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class PeriodoFinanceiroSpecification {

    private PeriodoFinanceiroSpecification() {
    }

    public static Specification<PeriodoFinanceiro> hasCurrentUserId(Long currentUserId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), currentUserId);
    }

    public static Specification<PeriodoFinanceiro> hasUserId(Long userId) {
        return (root, query, cb) -> userId == null
                ? null
                : cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<PeriodoFinanceiro> hasDataInicioFrom(LocalDate dataInicio) {
        return (root, query, cb) -> dataInicio == null
                ? null
                : cb.greaterThanOrEqualTo(root.get("dataInicio"), dataInicio);
    }

    public static Specification<PeriodoFinanceiro> hasDataFimTo(LocalDate dataFim) {
        return (root, query, cb) -> dataFim == null
                ? null
                : cb.lessThanOrEqualTo(root.get("dataFim"), dataFim);
    }

    public static Specification<PeriodoFinanceiro> hasSearchTerm(String searchTerm) {
        return (root, query, cb) -> {
            if (searchTerm == null || searchTerm.isBlank()) {
                return null;
            }

            String normalizedSearchTerm = "%" + searchTerm.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.join("user", JoinType.INNER).get("nome")), normalizedSearchTerm);
        };
    }
}
