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

    @EntityGraph(attributePaths = {"categoria", "user"})
    List<TransacaoRecorrente> findAllByUserId(Long userId);

    boolean existsByCategoriaIdAndUserId(Long categoriaId, Long userId);

    @EntityGraph(attributePaths = {"categoria", "user"})
    @Query("""
            select transacaoRecorrente as recorrente,
                   (select count(transacao.id)
                    from Transacao transacao
                    where transacao.transacaoRecorrente = transacaoRecorrente
                      and transacao.user.id = :userId) as parcelasLancadas,
                   (select max(transacao.data)
                    from Transacao transacao
                    where transacao.transacaoRecorrente = transacaoRecorrente
                      and transacao.user.id = :userId) as ultimaData
            from TransacaoRecorrente transacaoRecorrente
            where transacaoRecorrente.user.id = :userId
            """)
    List<TransacaoRecorrenteUsageProjection> findUsageByUserId(@Param("userId") Long userId);

    @Query("""
            select distinct transacaoRecorrente.categoria.id
            from TransacaoRecorrente transacaoRecorrente
            where transacaoRecorrente.user.id = :userId
              and transacaoRecorrente.categoria.id in :categoriaIds
            """)
    List<Long> findCategoriaIdsByUserIdAndCategoriaIds(
            @Param("userId") Long userId,
            @Param("categoriaIds") Collection<Long> categoriaIds
    );
}
