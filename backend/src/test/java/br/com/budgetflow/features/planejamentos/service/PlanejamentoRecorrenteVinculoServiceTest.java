package br.com.budgetflow.features.planejamentos.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.budgetflow.common.enums.StatusRecorrencia;
import br.com.budgetflow.common.exceptions.BusinessRuleException;
import br.com.budgetflow.features.movimentacoes.domain.TransacaoRecorrente;
import br.com.budgetflow.features.movimentacoes.repository.TransacaoRecorrenteRepository;
import br.com.budgetflow.features.movimentacoes.service.FinalizacaoRecorrenciaService;
import br.com.budgetflow.features.periodos.domain.PeriodoFinanceiro;
import br.com.budgetflow.features.planejamentos.repository.PlanejamentoRepository;

@ExtendWith(MockitoExtension.class)
class PlanejamentoRecorrenteVinculoServiceTest {

    @Mock
    private TransacaoRecorrenteRepository recorrenteRepository;
    @Mock
    private PlanejamentoRepository planejamentoRepository;
    @Mock
    private FinalizacaoRecorrenciaService finalizacaoRecorrenciaService;

    private PlanejamentoRecorrenteVinculoService service;

    @BeforeEach
    void setUp() {
        service = new PlanejamentoRecorrenteVinculoService(
                recorrenteRepository,
                planejamentoRepository,
                finalizacaoRecorrenciaService
        );
    }

    @ParameterizedTest
    @EnumSource(value = StatusRecorrencia.class, names = {"INATIVA", "FINALIZADA"})
    void naoPermiteAdicionarRecorrenciaQueNaoEstejaAtiva(StatusRecorrencia status) {
        PeriodoFinanceiro periodo = mock(PeriodoFinanceiro.class);
        TransacaoRecorrente recorrente = mock(TransacaoRecorrente.class);
        when(recorrente.getStatus()).thenReturn(status);
        when(recorrenteRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(recorrente));

        assertThrows(
                BusinessRuleException.class,
                () -> service.resolverChaveSincronizacao(20L, periodo, 1L)
        );

        verify(finalizacaoRecorrenciaService).finalizarExpiradas(1L);
        verifyNoInteractions(planejamentoRepository);
    }
}
