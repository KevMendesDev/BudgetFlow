package br.com.budgetflow.features.movimentacoes.repository;

import br.com.budgetflow.features.movimentacoes.domain.TransacaoRecorrente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransacaoRecorrenteRepository extends JpaRepository<TransacaoRecorrente, Long> {
}
