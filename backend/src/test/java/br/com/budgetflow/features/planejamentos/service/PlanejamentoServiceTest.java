package br.com.budgetflow.features.planejamentos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import br.com.budgetflow.common.enums.Frequencia;
import br.com.budgetflow.common.enums.NaturezaFinanceira;
import br.com.budgetflow.features.categorias.repository.CategoriaRepository;
import br.com.budgetflow.features.movimentacoes.domain.TransacaoRecorrente;
import br.com.budgetflow.features.movimentacoes.repository.TransacaoRecorrenteRepository;
import br.com.budgetflow.features.periodos.domain.PeriodoFinanceiro;
import br.com.budgetflow.features.periodos.service.PeriodoFinanceiroService;
import br.com.budgetflow.features.planejamentos.domain.Planejamento;
import br.com.budgetflow.features.planejamentos.mapper.PlanejamentoMapper;
import br.com.budgetflow.features.planejamentos.repository.PlanejamentoRepository;
import br.com.budgetflow.features.users.domain.User;
import br.com.budgetflow.features.users.service.UserService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanejamentoServiceTest {

    @Mock
    private PlanejamentoRepository planejamentoRepository;
    @Mock
    private TransacaoRecorrenteRepository recorrenteRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private PeriodoFinanceiroService periodoFinanceiroService;
    @Mock
    private UserService userService;
    @Mock
    private PlanejamentoMapper planejamentoMapper;

    private PlanejamentoService service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", null, List.of())
        );
        service = new PlanejamentoService(
                planejamentoRepository,
                recorrenteRepository,
                categoriaRepository,
                periodoFinanceiroService,
                userService,
                planejamentoMapper
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sincronizacaoGeraOcorrenciasIdempotentesEIgnoraRecorrenciaSemValor() {
        User user = mock(User.class);
        PeriodoFinanceiro periodo = mock(PeriodoFinanceiro.class);
        when(periodo.getUser()).thenReturn(user);
        when(periodo.getDataInicio()).thenReturn(LocalDate.of(2026, 6, 1));
        when(periodo.getDataFim()).thenReturn(LocalDate.of(2026, 6, 30));
        when(periodoFinanceiroService.resolvePeriodoToTransacao(10L, 1L)).thenReturn(periodo);

        TransacaoRecorrente mensal = recorrente(20L, user, LocalDate.of(2026, 5, 10), BigDecimal.TEN);
        TransacaoRecorrente semValor = recorrente(21L, user, LocalDate.of(2026, 6, 15), null);
        when(recorrenteRepository.findAllByUserId(1L)).thenReturn(List.of(mensal, semValor));

        Set<String> chavesPersistidas = new HashSet<>();
        when(planejamentoRepository.existsByChaveSincronizacaoAndUserId(any(), anyLong()))
                .thenAnswer(invocation -> chavesPersistidas.contains(invocation.getArgument(0)));
        when(planejamentoRepository.saveAll(any())).thenAnswer(invocation -> {
            List<Planejamento> items = invocation.getArgument(0);
            items.forEach(item -> chavesPersistidas.add(item.getChaveSincronizacao()));
            return items;
        });

        var primeira = service.sincronizarRecorrentes(10L);
        var segunda = service.sincronizarRecorrentes(10L);

        assertEquals(1, primeira.planejamentosGerados());
        assertEquals(1, primeira.recorrenciasSemValor());
        assertEquals(0, segunda.planejamentosGerados());

        ArgumentCaptor<Iterable<Planejamento>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(planejamentoRepository, org.mockito.Mockito.times(2)).saveAll(captor.capture());
        Planejamento gerado = captor.getAllValues().getFirst().iterator().next();
        assertTrue(gerado.isSincronizado());
    }

    @Test
    void exclusaoDeSincronizadoMantemTombstone() {
        Planejamento planejamento = new Planejamento();
        planejamento.setChaveSincronizacao("hash");
        when(planejamentoRepository.findByIdAndUserIdAndExcluidoFalse(5L, 1L))
                .thenReturn(Optional.of(planejamento));

        service.delete(5L);

        assertTrue(planejamento.isExcluido());
        verify(planejamentoRepository).save(planejamento);
        verify(planejamentoRepository, never()).delete(planejamento);
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
