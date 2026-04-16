package br.com.budgetflow.features.periodos.repository;

import br.com.budgetflow.features.periodos.domain.PeriodoFinanceiro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Optional;

public interface PeriodoFinanceiroRepository extends JpaRepository<PeriodoFinanceiro, Long>, JpaSpecificationExecutor<PeriodoFinanceiro> {
	Page<PeriodoFinanceiro> findAllByUserId(Long userId, Pageable pageable);

	Optional<PeriodoFinanceiro> findFirstByUserIdAndDataInicioLessThanEqualAndDataFimGreaterThanEqual(Long userId, LocalDate dataInicio, LocalDate dataFim);
}
