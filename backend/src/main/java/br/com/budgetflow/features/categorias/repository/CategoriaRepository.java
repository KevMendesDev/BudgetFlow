package br.com.budgetflow.features.categorias.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.budgetflow.common.enums.ClassificacaoCategoria;
import br.com.budgetflow.features.categorias.domain.Categoria;

import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long>, JpaSpecificationExecutor<Categoria> {

    Page<Categoria> findAllByUserId(Long userId, Pageable pageable);

    Optional<Categoria> findByIdAndUserId(Long id, Long userId);

    boolean existsByNomeIgnoreCaseAndUserIdAndClassificacao(String nome, Long userId, ClassificacaoCategoria classificacao);

    boolean existsByNomeIgnoreCaseAndUserIdAndClassificacaoAndIdNot(String nome, Long userId, ClassificacaoCategoria classificacao, Long id);

}
