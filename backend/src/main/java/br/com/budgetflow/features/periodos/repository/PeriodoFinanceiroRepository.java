package br.com.budgetflow.features.periodos.repository;

import br.com.budgetflow.features.periodos.domain.PeriodoFinanceiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Optional;

public interface PeriodoFinanceiroRepository extends JpaRepository<PeriodoFinanceiro, Long>, JpaSpecificationExecutor<PeriodoFinanceiro> {

    Optional<PeriodoFinanceiro> findByIdAndUserId(Long id, Long userId);

    Optional<PeriodoFinanceiro> findFirstByUserIdAndDataInicioLessThanEqualAndDataFimGreaterThanEqual(Long userId, LocalDate dataInicio, LocalDate dataFim);

    boolean existsByUserIdAndDataInicio(Long userId, LocalDate dataInicio);

    boolean existsByUserIdAndDataInicioAndIdNot(Long userId, LocalDate dataInicio, Long id);
}
