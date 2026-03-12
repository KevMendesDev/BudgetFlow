package br.com.budgetflow.features.movimentacoes.repository;

import br.com.budgetflow.features.movimentacoes.domain.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    boolean existsByTransacaoRecorrenteIdAndData(Long transacaoRecorrenteId, LocalDate data);
}
