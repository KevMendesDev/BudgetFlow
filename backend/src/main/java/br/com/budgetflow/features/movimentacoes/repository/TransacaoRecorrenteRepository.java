package br.com.budgetflow.features.movimentacoes.repository;

import br.com.budgetflow.features.movimentacoes.domain.TransacaoRecorrente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface TransacaoRecorrenteRepository extends JpaRepository<TransacaoRecorrente, Long>, JpaSpecificationExecutor<TransacaoRecorrente> {

	Optional<TransacaoRecorrente> findByIdAndUserId(Long id, Long userId);

	List<TransacaoRecorrente> findAllByUserId(Long userId);
}
