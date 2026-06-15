package br.com.budgetflow.features.planejamentos.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.budgetflow.features.planejamentos.domain.Planejamento;

public interface PlanejamentoRepository extends JpaRepository<Planejamento, Long> {

    List<Planejamento> findAllByPeriodoIdAndUserIdAndExcluidoFalseOrderByCreatedAtDescIdDesc(Long periodoId, Long userId);

    Optional<Planejamento> findByIdAndUserIdAndExcluidoFalse(Long id, Long userId);

    boolean existsByChaveSincronizacaoAndUserId(String chaveSincronizacao, Long userId);

    boolean existsByCategoriaIdAndUserIdAndExcluidoFalse(Long categoriaId, Long userId);

    boolean existsByPeriodoIdAndUserIdAndExcluidoFalse(Long periodoId, Long userId);

    @Query("""
            select distinct p.categoria.id
            from Planejamento p
            where p.user.id = :userId
              and p.categoria.id in :categoriaIds
              and p.excluido = false
            """)
    List<Long> findCategoriaIdsByUserIdAndCategoriaIds(
            @Param("userId") Long userId,
            @Param("categoriaIds") Collection<Long> categoriaIds
    );

    @Query("""
            select distinct p.periodo.id
            from Planejamento p
            where p.user.id = :userId
              and p.periodo.id in :periodoIds
              and p.excluido = false
            """)
    List<Long> findPeriodoIdsByUserIdAndPeriodoIds(
            @Param("userId") Long userId,
            @Param("periodoIds") Collection<Long> periodoIds
    );
}
