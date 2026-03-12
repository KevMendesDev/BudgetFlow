package br.com.budgetflow.features.periodos.repository;

import br.com.budgetflow.features.periodos.domain.PeriodoFinanceiro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeriodoFinanceiroRepository extends JpaRepository<PeriodoFinanceiro, Long> {
}
