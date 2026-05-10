package br.com.budgetflow.features.movimentacoes.repository;

import br.com.budgetflow.features.movimentacoes.domain.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Optional;

public interface TransacaoRepository extends JpaRepository<Transacao, Long>, JpaSpecificationExecutor<Transacao> {

    boolean existsByTransacaoRecorrenteIdAndData(Long transacaoRecorrenteId, LocalDate data);

    boolean existsByTransacaoRecorrenteIdAndDataAndUserId(Long transacaoRecorrenteId, LocalDate data, Long userId);

    boolean existsByTransacaoRecorrenteIdAndPeriodoIdAndUserId(Long transacaoRecorrenteId, Long periodoId, Long userId);

    boolean existsByTransacaoRecorrenteIdAndPeriodoIdAndUserIdAndIdNot(Long transacaoRecorrenteId, Long periodoId, Long userId, Long id);

    long countByTransacaoRecorrenteId(Long transacaoRecorrenteId);

    long countByTransacaoRecorrenteIdAndUserId(Long transacaoRecorrenteId, Long userId);

    long countByTransacaoRecorrenteIdAndUserIdAndIdNot(Long transacaoRecorrenteId, Long userId, Long id);

    boolean existsByCategoriaIdAndUserId(Long categoriaId, Long userId);

    boolean existsByPeriodoIdAndUserId(Long periodoId, Long userId);

    boolean existsByTransacaoRecorrenteIdAndUserId(Long transacaoRecorrenteId, Long userId);

    Optional<Transacao> findByIdAndUserId(Long id, Long userId);
}
