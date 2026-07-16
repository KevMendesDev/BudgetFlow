package br.com.budgetflow.features.planejamentos.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.budgetflow.common.enums.StatusRecorrencia;
import br.com.budgetflow.common.exceptions.BusinessRuleException;
import br.com.budgetflow.common.exceptions.ResourceNotFoundException;
import br.com.budgetflow.features.categorias.domain.Categoria;
import br.com.budgetflow.features.categorias.repository.CategoriaRepository;
import br.com.budgetflow.features.movimentacoes.domain.TransacaoRecorrente;
import br.com.budgetflow.features.movimentacoes.repository.TransacaoRecorrenteRepository;
import br.com.budgetflow.features.movimentacoes.service.support.RecorrenciaUtils;
import br.com.budgetflow.features.periodos.domain.PeriodoFinanceiro;
import br.com.budgetflow.features.periodos.service.PeriodoFinanceiroService;
import br.com.budgetflow.features.planejamentos.domain.Planejamento;
import br.com.budgetflow.features.planejamentos.dto.PlanejamentoRequestDTO;
import br.com.budgetflow.features.planejamentos.dto.PlanejamentoResponseDTO;
import br.com.budgetflow.features.planejamentos.dto.SincronizacaoPlanejamentosResponseDTO;
import br.com.budgetflow.features.planejamentos.mapper.PlanejamentoMapper;
import br.com.budgetflow.features.planejamentos.repository.PlanejamentoRepository;
import br.com.budgetflow.features.users.domain.User;
import br.com.budgetflow.features.users.service.UserService;
import br.com.budgetflow.security.SecurityUtils;

@Service
public class PlanejamentoService {

    private final PlanejamentoRepository planejamentoRepository;
    private final TransacaoRecorrenteRepository recorrenteRepository;
    private final CategoriaRepository categoriaRepository;
    private final PeriodoFinanceiroService periodoFinanceiroService;
    private final UserService userService;
    private final PlanejamentoMapper planejamentoMapper;

    public PlanejamentoService(
            PlanejamentoRepository planejamentoRepository,
            TransacaoRecorrenteRepository recorrenteRepository,
            CategoriaRepository categoriaRepository,
            PeriodoFinanceiroService periodoFinanceiroService,
            UserService userService,
            PlanejamentoMapper planejamentoMapper
    ) {
        this.planejamentoRepository = planejamentoRepository;
        this.recorrenteRepository = recorrenteRepository;
        this.categoriaRepository = categoriaRepository;
        this.periodoFinanceiroService = periodoFinanceiroService;
        this.userService = userService;
        this.planejamentoMapper = planejamentoMapper;
    }

    @Transactional(readOnly = true)
    public List<PlanejamentoResponseDTO> findAll(Long periodoId) {
        Long userId = SecurityUtils.currentUserId();
        PeriodoFinanceiro periodo = periodoFinanceiroService.resolvePeriodoToTransacao(periodoId, userId);
        return planejamentoRepository
                .findAllByPeriodoIdAndUserIdAndExcluidoFalseOrderByCreatedAtDescIdDesc(periodo.getId(), userId)
                .stream()
                .map(planejamentoMapper::toResponseDTO)
                .toList();
    }

    @Transactional
    public PlanejamentoResponseDTO create(PlanejamentoRequestDTO request) {
        Long userId = SecurityUtils.currentUserId();
        User user = userService.findById(userId);
        PeriodoFinanceiro periodo = periodoFinanceiroService.resolvePeriodoToTransacao(request.periodoId(), userId);
        Categoria categoria = resolveCategoria(request, userId);

        Planejamento planejamento = new Planejamento();
        planejamento.setUser(user);
        planejamento.setPeriodo(periodo);
        fillEditableFields(planejamento, request, categoria);

        return planejamentoMapper.toResponseDTO(planejamentoRepository.save(planejamento));
    }

    @Transactional
    public PlanejamentoResponseDTO update(Long id, PlanejamentoRequestDTO request) {
        Long userId = SecurityUtils.currentUserId();
        Planejamento planejamento = findEntity(id, userId);
        PeriodoFinanceiro periodo = periodoFinanceiroService.resolvePeriodoToTransacao(request.periodoId(), userId);
        Categoria categoria = resolveCategoria(request, userId);

        planejamento.setPeriodo(periodo);
        fillEditableFields(planejamento, request, categoria);

        return planejamentoMapper.toResponseDTO(planejamentoRepository.save(planejamento));
    }

    @Transactional
    public void delete(Long id) {
        Long userId = SecurityUtils.currentUserId();
        Planejamento planejamento = findEntity(id, userId);

        if (planejamento.isSincronizado()) {
            planejamento.setExcluido(true);
            planejamentoRepository.save(planejamento);
            return;
        }

        planejamentoRepository.delete(planejamento);
    }

    @Transactional
    public SincronizacaoPlanejamentosResponseDTO sincronizarRecorrentes(Long periodoId) {
        Long userId = SecurityUtils.currentUserId();
        PeriodoFinanceiro periodo = periodoFinanceiroService.resolvePeriodoToTransacao(periodoId, userId);
        finalizarExpiradas(userId);
        List<Planejamento> novos = new ArrayList<>();
        Set<String> chavesSincronizadas = new HashSet<>(
                planejamentoRepository.findChavesSincronizacaoByPeriodoIdAndUserId(periodo.getId(), userId)
        );
        int semValor = 0;

        for (TransacaoRecorrente recorrente : recorrenteRepository.findAllByUserIdAndStatus(userId, StatusRecorrencia.ATIVA)) {
            if (recorrente.getValor() == null) {
                semValor++;
                continue;
            }
            gerarOcorrenciasDoPeriodo(recorrente, periodo, userId, chavesSincronizadas, novos);
        }

        planejamentoRepository.saveAll(novos);
        String mensagem = "Sincronização concluída: " + novos.size()
                + " planejamentos gerados e " + semValor + " recorrências sem valor.";
        return new SincronizacaoPlanejamentosResponseDTO(novos.size(), semValor, mensagem);
    }

    private void gerarOcorrenciasDoPeriodo(
            TransacaoRecorrente recorrente,
            PeriodoFinanceiro periodo,
            Long userId,
            Set<String> chavesSincronizadas,
            List<Planejamento> novos
    ) {
        long indice = 0;
        while (recorrente.getTotalParcelas() == null || indice < recorrente.getTotalParcelas()) {
            LocalDate data = RecorrenciaUtils.calcularDataOcorrencia(
                    recorrente.getDataInicio(),
                    recorrente.getFrequencia(),
                    indice
            );

            if (recorrente.getDataFim() != null && data.isAfter(recorrente.getDataFim())) {
                break;
            }
            if (data.isAfter(periodo.getDataFim())) {
                break;
            }

            if (!data.isBefore(periodo.getDataInicio())) {
                String chave = buildSyncKey(userId, recorrente.getId(), data);
                if (chavesSincronizadas.add(chave)) {
                    Planejamento planejamento = new Planejamento();
                    planejamento.setUser(recorrente.getUser());
                    planejamento.setPeriodo(periodo);
                    planejamento.setCategoria(recorrente.getCategoria());
                    planejamento.setDescricao(recorrente.getDescricao());
                    planejamento.setValor(recorrente.getValor());
                    planejamento.setTipoMovimentacao(recorrente.getTipoMovimentacao());
                    planejamento.setChaveSincronizacao(chave);
                    novos.add(planejamento);
                }
            }
            indice++;
        }
    }

    private Categoria resolveCategoria(PlanejamentoRequestDTO request, Long userId) {
        Categoria categoria = categoriaRepository.findByIdAndUserId(request.categoriaId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));

        if (categoria.getTipoCategoria() != request.tipoMovimentacao()) {
            throw new BusinessRuleException("O tipo da categoria deve ser igual ao tipo de movimentação");
        }
        return categoria;
    }

    private void fillEditableFields(
            Planejamento planejamento,
            PlanejamentoRequestDTO request,
            Categoria categoria
    ) {
        planejamento.setCategoria(categoria);
        planejamento.setDescricao(request.descricao().trim());
        planejamento.setValor(request.valor());
        planejamento.setTipoMovimentacao(request.tipoMovimentacao());
    }

    private Planejamento findEntity(Long id, Long userId) {
        return planejamentoRepository.findByIdAndUserIdAndExcluidoFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Planejamento não encontrado"));
    }

    private String buildSyncKey(Long userId, Long recorrenteId, LocalDate data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String source = userId + ":" + recorrenteId + ":" + data;
            return HexFormat.of().formatHex(digest.digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível", ex);
        }
    }

    private void finalizarExpiradas(Long userId) {
        List<TransacaoRecorrente> expiradas = recorrenteRepository.findExpiradasNaoFinalizadas(
                userId,
                LocalDate.now(),
                StatusRecorrencia.FINALIZADA
        );
        for (TransacaoRecorrente recorrente : expiradas) {
            recorrente.setStatus(StatusRecorrencia.FINALIZADA);
        }
        if (!expiradas.isEmpty()) {
            recorrenteRepository.saveAll(expiradas);
        }
    }
}
