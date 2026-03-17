package br.com.budgetflow.features.categorias.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.budgetflow.common.enums.ClassificacaoCategoria;
import br.com.budgetflow.features.categorias.domain.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    Page<Categoria> findAllByUserId(Long userId, Pageable pageable);
	boolean existsByNomeIgnoreCaseAndUserIdAndClassificacao(String nome, Long userId, ClassificacaoCategoria classificacao);

	boolean existsByNomeIgnoreCaseAndUserIdAndClassificacaoAndIdNot(String nome, Long userId, ClassificacaoCategoria classificacao, Long id);

}
