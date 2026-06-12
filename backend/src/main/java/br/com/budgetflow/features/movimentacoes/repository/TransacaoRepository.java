package br.com.budgetflow.features.movimentacoes.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.budgetflow.features.movimentacoes.domain.Transacao;

public interface TransacaoRepository extends JpaRepository<Transacao, Long>, JpaSpecificationExecutor<Transacao> {

    boolean existsByTransacaoRecorrenteIdAndData(Long transacaoRecorrenteId, LocalDate data);

    boolean existsByTransacaoRecorrenteIdAndDataAndUserId(Long transacaoRecorrenteId, LocalDate data, Long userId);

    boolean existsByTransacaoRecorrenteIdAndPeriodoIdAndUserId(Long transacaoRecorrenteId, Long periodoId, Long userId);

    boolean existsByTransacaoRecorrenteIdAndPeriodoIdAndUserIdAndIdNot(Long transacaoRecorrenteId, Long periodoId, Long userId, Long id);

    long countByTransacaoRecorrenteId(Long transacaoRecorrenteId);

    long countByTransacaoRecorrenteIdAndUserId(Long transacaoRecorrenteId, Long userId);

    long countByTransacaoRecorrenteIdAndUserIdAndIdNot(Long transacaoRecorrenteId, Long userId, Long id);

    long countByTransacaoRecorrenteIdAndUserIdAndDataLessThanEqual(Long transacaoRecorrenteId, Long userId, LocalDate data);

    long countByTransacaoRecorrenteIdAndUserIdAndDataLessThanEqualAndIdNot(Long transacaoRecorrenteId, Long userId, LocalDate data, Long id);

    boolean existsByCategoriaIdAndUserId(Long categoriaId, Long userId);

    @Query("""
            select distinct t.categoria.id
            from Transacao t
            where t.user.id = :userId
              and t.categoria.id in :categoriaIds
            """)
    List<Long> findCategoriaIdsByUserIdAndCategoriaIds(
            @Param("userId") Long userId,
            @Param("categoriaIds") Collection<Long> categoriaIds
    );

    @Query("""
            select distinct t.periodo.id
            from Transacao t
            where t.user.id = :userId
              and t.periodo.id in :periodoIds
            """)
    List<Long> findPeriodoIdsByUserIdAndPeriodoIds(
            @Param("userId") Long userId,
            @Param("periodoIds") Collection<Long> periodoIds
    );

    @Query("""
            select distinct t.transacaoRecorrente.id
            from Transacao t
            where t.user.id = :userId
              and t.transacaoRecorrente.id in :transacaoRecorrenteIds
            """)
    List<Long> findTransacaoRecorrenteIdsByUserIdAndTransacaoRecorrenteIds(
            @Param("userId") Long userId,
            @Param("transacaoRecorrenteIds") Collection<Long> transacaoRecorrenteIds
    );

    boolean existsByPeriodoIdAndUserId(Long periodoId, Long userId);

    boolean existsByTransacaoRecorrenteIdAndUserId(Long transacaoRecorrenteId, Long userId);

    Optional<Transacao> findByIdAndUserId(Long id, Long userId);

    Optional<Transacao> findFirstByTransacaoRecorrenteIdAndUserIdOrderByDataDescIdDesc(Long transacaoRecorrenteId, Long userId);

    Optional<Transacao> findFirstByTransacaoRecorrenteIdAndUserIdAndIdNotAndDataLessThanEqualOrderByDataDescIdDesc(
            Long transacaoRecorrenteId,
            Long userId,
            Long id,
            LocalDate data
    );

    Optional<Transacao> findFirstByTransacaoRecorrenteIdAndUserIdAndIdNotAndDataGreaterThanEqualOrderByDataAscIdAsc(
            Long transacaoRecorrenteId,
            Long userId,
            Long id,
            LocalDate data
    );
}
