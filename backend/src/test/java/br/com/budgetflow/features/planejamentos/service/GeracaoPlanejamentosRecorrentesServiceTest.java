package br.com.budgetflow.features.planejamentos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import br.com.budgetflow.common.enums.Frequencia;
import br.com.budgetflow.common.enums.NaturezaFinanceira;
import br.com.budgetflow.common.enums.StatusRecorrencia;
import br.com.budgetflow.features.movimentacoes.domain.TransacaoRecorrente;
import br.com.budgetflow.features.movimentacoes.repository.TransacaoRecorrenteRepository;
import br.com.budgetflow.features.periodos.domain.PeriodoFinanceiro;
import br.com.budgetflow.features.planejamentos.domain.Planejamento;
import br.com.budgetflow.features.planejamentos.repository.PlanejamentoRepository;
import br.com.budgetflow.features.users.domain.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GeracaoPlanejamentosRecorrentesServiceTest {

    @Mock
    private TransacaoRecorrenteRepository recorrenteRepository;
    @Mock
    private PlanejamentoRepository planejamentoRepository;

    private GeracaoPlanejamentosRecorrentesService service;

    @BeforeEach
    void setUp() {
        service = new GeracaoPlanejamentosRecorrentesService(recorrenteRepository, planejamentoRepository);
    }

    @Test
    void sincronizacaoGeraOcorrenciasIdempotentesEIgnoraRecorrenciaSemValor() {
        User user = mock(User.class);
        PeriodoFinanceiro periodo = mock(PeriodoFinanceiro.class);
        when(periodo.getUser()).thenReturn(user);
        when(periodo.getId()).thenReturn(10L);
        when(periodo.getDataInicio()).thenReturn(LocalDate.of(2026, 6, 1));
        when(periodo.getDataFim()).thenReturn(LocalDate.of(2026, 6, 30));

        TransacaoRecorrente mensal = recorrente(20L, user, LocalDate.of(2026, 5, 10), BigDecimal.TEN);
        TransacaoRecorrente semValor = recorrente(21L, user, LocalDate.of(2026, 6, 15), null);
        when(recorrenteRepository.findAllByUserIdAndStatus(1L, StatusRecorrencia.ATIVA))
                .thenReturn(List.of(mensal, semValor));

        Set<String> chavesPersistidas = new HashSet<>();
        when(planejamentoRepository.findChavesSincronizacaoByPeriodoIdAndUserId(10L, 1L))
                .thenAnswer(invocation -> new HashSet<>(chavesPersistidas));
        when(planejamentoRepository.saveAll(any())).thenAnswer(invocation -> {
            List<Planejamento> items = invocation.getArgument(0);
            items.forEach(item -> chavesPersistidas.add(item.getChaveSincronizacao()));
            return items;
        });

        var primeira = service.sincronizar(periodo, 1L);
        var segunda = service.sincronizar(periodo, 1L);

        assertEquals(1, primeira.planejamentosGerados());
        assertEquals(1, primeira.recorrenciasSemValor());
        assertEquals(0, segunda.planejamentosGerados());

        ArgumentCaptor<Iterable<Planejamento>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(planejamentoRepository, times(2)).saveAll(captor.capture());
        verify(planejamentoRepository, times(2))
                .findChavesSincronizacaoByPeriodoIdAndUserId(10L, 1L);
        verify(recorrenteRepository, times(2))
                .findAllByUserIdAndStatus(1L, StatusRecorrencia.ATIVA);
        Planejamento gerado = captor.getAllValues().getFirst().iterator().next();
        assertTrue(gerado.isSincronizado());
    }

    private TransacaoRecorrente recorrente(Long id, User user, LocalDate inicio, BigDecimal valor) {
        TransacaoRecorrente recorrente = mock(TransacaoRecorrente.class);
        when(recorrente.getId()).thenReturn(id);
        when(recorrente.getUser()).thenReturn(user);
        when(recorrente.getDataInicio()).thenReturn(inicio);
        when(recorrente.getFrequencia()).thenReturn(Frequencia.MENSAL);
        when(recorrente.getTotalParcelas()).thenReturn(null);
        when(recorrente.getValor()).thenReturn(valor);
        when(recorrente.getDescricao()).thenReturn("Recorrente");
        when(recorrente.getTipoMovimentacao()).thenReturn(NaturezaFinanceira.DESPESA);
        return recorrente;
    }
}
