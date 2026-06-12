package br.com.budgetflow.features.movimentacoes.service.support;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;

import br.com.budgetflow.common.enums.Frequencia;

class RecorrenciaUtilsTest {

    @Test
    void devePermitirAtualizarOcorrenciaMantendoIntervaloComAsVizinhas() {
        assertDoesNotThrow(() -> RecorrenciaUtils.validarIntervaloEntreRecorrencias(
                LocalDate.of(2026, 2, 10),
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 3, 10),
                Frequencia.MENSAL
        ));
    }
}
