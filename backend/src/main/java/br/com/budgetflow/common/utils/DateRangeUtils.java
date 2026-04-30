package br.com.budgetflow.common.utils;

import br.com.budgetflow.common.exceptions.BusinessRuleException;

import java.time.LocalDate;

public final class DateRangeUtils {

    private DateRangeUtils() {
    }

    public static void validateRange(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio != null && dataFim != null && dataFim.isBefore(dataInicio)) {
            throw new BusinessRuleException("A data de fim não pode ser anterior à data de início");
        }
    }
}
