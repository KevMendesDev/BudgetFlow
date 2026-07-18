package br.com.budgetflow.features.planejamentos.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import br.com.budgetflow.features.categorias.repository.CategoriaRepository;
import br.com.budgetflow.features.periodos.service.PeriodoFinanceiroService;
import br.com.budgetflow.features.planejamentos.domain.Planejamento;
import br.com.budgetflow.features.planejamentos.mapper.PlanejamentoMapper;
import br.com.budgetflow.features.planejamentos.repository.PlanejamentoRepository;
import br.com.budgetflow.features.users.service.UserService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanejamentoServiceTest {

    @Mock
    private PlanejamentoRepository planejamentoRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private PeriodoFinanceiroService periodoFinanceiroService;
    @Mock
    private UserService userService;
    @Mock
    private PlanejamentoMapper planejamentoMapper;
    @Mock
    private PlanejamentoRecorrenteVinculoService recorrenteVinculoService;
    @Mock
    private GeracaoPlanejamentosRecorrentesService geracaoPlanejamentosRecorrentesService;

    private PlanejamentoService service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", null, List.of())
        );
        service = new PlanejamentoService(
                planejamentoRepository,
                categoriaRepository,
                periodoFinanceiroService,
                userService,
                planejamentoMapper,
                recorrenteVinculoService,
                geracaoPlanejamentosRecorrentesService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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
}
