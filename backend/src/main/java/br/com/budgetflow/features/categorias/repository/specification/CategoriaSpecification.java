package br.com.budgetflow.features.categorias.repository.specification;

import br.com.budgetflow.common.enums.ClassificacaoCategoria;
import br.com.budgetflow.common.enums.NaturezaFinanceira;
import br.com.budgetflow.features.categorias.domain.Categoria;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class CategoriaSpecification {

    private CategoriaSpecification() {
    }

    public static Specification<Categoria> hasUserId(Long userId) {
        return (root, query, cb) -> userId == null
                ? null
                : cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Categoria> hasClassificacao(ClassificacaoCategoria classificacao) {
        return (root, query, cb) -> classificacao == null
                ? null
                : cb.equal(root.get("classificacao"), classificacao);
    }

    public static Specification<Categoria> hasTipoCategoria(NaturezaFinanceira tipoCategoria) {
        return (root, query, cb) -> tipoCategoria == null
                ? null
                : cb.equal(root.get("tipoCategoria"), tipoCategoria);
    }

    public static Specification<Categoria> hasNomeUsuario(String nomeUsuario) {
        return (root, query, cb) -> {
            if (nomeUsuario == null || nomeUsuario.isBlank()) {
                return null;
            }

            String normalizedNomeUsuario = "%" + nomeUsuario.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.join("user", JoinType.INNER).get("nome")), normalizedNomeUsuario);
        };
    }

    public static Specification<Categoria> hasSearchTerm(String searchTerm) {
        return (root, query, cb) -> {
            if (searchTerm == null || searchTerm.isBlank()) {
                return null;
            }

            String normalizedSearchTerm = "%" + searchTerm.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("nome")), normalizedSearchTerm),
                    cb.like(cb.lower(root.join("user", JoinType.INNER).get("nome")), normalizedSearchTerm)
            );
        };
    }
}
