package br.com.budgetflow.features.movimentacoes.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.budgetflow.features.movimentacoes.domain.TransacaoRecorrente;
import br.com.budgetflow.features.movimentacoes.repository.projection.TransacaoRecorrenteUsageProjection;

public interface TransacaoRecorrenteRepository extends JpaRepository<TransacaoRecorrente, Long>, JpaSpecificationExecutor<TransacaoRecorrente> {

    Optional<TransacaoRecorrente> findByIdAndUserId(Long id, Long userId);

    boolean existsByCategoriaIdAndUserId(Long categoriaId, Long userId);

    @EntityGraph(attributePaths = {"categoria", "user"})
    @Query("""
            select tr as recorrente,
                   count(transacao.id) as parcelasLancadas,
                   max(transacao.data) as ultimaData
            from TransacaoRecorrente tr
            left join Transacao transacao
              on transacao.transacaoRecorrente = tr
             and transacao.user.id = :userId
            where tr.user.id = :userId
            group by tr
            """)
    List<TransacaoRecorrenteUsageProjection> findUsageByUserId(@Param("userId") Long userId);

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
