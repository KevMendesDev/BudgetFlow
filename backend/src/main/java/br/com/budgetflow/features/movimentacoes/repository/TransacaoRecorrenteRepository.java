package br.com.budgetflow.features.movimentacoes.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.budgetflow.features.movimentacoes.domain.TransacaoRecorrente;

public interface TransacaoRecorrenteRepository extends JpaRepository<TransacaoRecorrente, Long>, JpaSpecificationExecutor<TransacaoRecorrente> {

    Optional<TransacaoRecorrente> findByIdAndUserId(Long id, Long userId);

    List<TransacaoRecorrente> findAllByUserId(Long userId);

    boolean existsByCategoriaIdAndUserId(Long categoriaId, Long userId);

    @Query("""
            select distinct tr.categoria.id
            from TransacaoRecorrente tr
            where tr.user.id = :userId
              and tr.categoria.id in :categoriaIds
            """)
    List<Long> findCategoriaIdsByUserIdAndCategoriaIds(
            @Param("userId") Long userId,
            @Param("categoriaIds") Collection<Long> categoriaIds
    );
}
