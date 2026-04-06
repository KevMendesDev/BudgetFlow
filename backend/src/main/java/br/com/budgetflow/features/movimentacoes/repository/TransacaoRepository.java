package br.com.budgetflow.features.movimentacoes.repository;

import br.com.budgetflow.features.movimentacoes.domain.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Optional;

public interface TransacaoRepository extends JpaRepository<Transacao, Long>, JpaSpecificationExecutor<Transacao> {

    boolean existsByTransacaoRecorrenteIdAndData(Long transacaoRecorrenteId, LocalDate data);

    boolean existsByTransacaoRecorrenteIdAndDataAndUserId(Long transacaoRecorrenteId, LocalDate data, Long userId);

    long countByTransacaoRecorrenteId(Long transacaoRecorrenteId);

    Optional<Transacao> findByIdAndUserId(Long id, Long userId);
}
