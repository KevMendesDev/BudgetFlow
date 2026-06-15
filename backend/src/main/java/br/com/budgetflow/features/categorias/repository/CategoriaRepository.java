package br.com.budgetflow.features.categorias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.budgetflow.common.enums.NaturezaFinanceira;
import br.com.budgetflow.common.enums.ClassificacaoCategoria;
import br.com.budgetflow.features.categorias.domain.Categoria;

import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long>, JpaSpecificationExecutor<Categoria> {

    Optional<Categoria> findByIdAndUserId(Long id, Long userId);

    boolean existsByNomeIgnoreCaseAndUserIdAndTipoCategoria(String nome, Long userId, NaturezaFinanceira tipoCategoria);

    boolean existsByNomeIgnoreCaseAndUserIdAndTipoCategoriaAndIdNot(String nome, Long userId, NaturezaFinanceira tipoCategoria, Long id);

    boolean existsByNomeIgnoreCaseAndUserIdAndTipoCategoriaAndClassificacao(
            String nome,
            Long userId,
            NaturezaFinanceira tipoCategoria,
            ClassificacaoCategoria classificacao
    );

    boolean existsByNomeIgnoreCaseAndUserIdAndTipoCategoriaAndClassificacaoAndIdNot(
            String nome,
            Long userId,
            NaturezaFinanceira tipoCategoria,
            ClassificacaoCategoria classificacao,
            Long id
    );

}
