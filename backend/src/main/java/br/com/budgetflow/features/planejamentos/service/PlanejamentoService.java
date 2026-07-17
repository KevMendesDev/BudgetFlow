package br.com.budgetflow.features.planejamentos.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.budgetflow.common.exceptions.BusinessRuleException;
import br.com.budgetflow.common.exceptions.ResourceNotFoundException;
import br.com.budgetflow.features.categorias.domain.Categoria;
import br.com.budgetflow.features.categorias.repository.CategoriaRepository;
import br.com.budgetflow.features.periodos.domain.PeriodoFinanceiro;
import br.com.budgetflow.features.periodos.service.PeriodoFinanceiroService;
import br.com.budgetflow.features.planejamentos.criteria.PlanejamentoFilterCriteria;
import br.com.budgetflow.features.planejamentos.domain.Planejamento;
import br.com.budgetflow.features.planejamentos.dto.PlanejamentoRequestDTO;
import br.com.budgetflow.features.planejamentos.dto.PlanejamentoResponseDTO;
import br.com.budgetflow.features.planejamentos.dto.SincronizacaoPlanejamentosResponseDTO;
import br.com.budgetflow.features.planejamentos.mapper.PlanejamentoMapper;
import br.com.budgetflow.features.planejamentos.repository.PlanejamentoRepository;
import br.com.budgetflow.features.planejamentos.repository.specification.PlanejamentoSpecification;
import br.com.budgetflow.features.users.domain.User;
import br.com.budgetflow.features.users.service.UserService;
import br.com.budgetflow.security.SecurityUtils;

@Service
public class PlanejamentoService {

    private final PlanejamentoRepository planejamentoRepository;
    private final CategoriaRepository categoriaRepository;
    private final PeriodoFinanceiroService periodoFinanceiroService;
    private final UserService userService;
    private final PlanejamentoMapper planejamentoMapper;
    private final PlanejamentoRecorrenteVinculoService recorrenteVinculoService;
    private final GeracaoPlanejamentosRecorrentesService geracaoPlanejamentosRecorrentesService;

    public PlanejamentoService(
            PlanejamentoRepository planejamentoRepository,
            CategoriaRepository categoriaRepository,
            PeriodoFinanceiroService periodoFinanceiroService,
            UserService userService,
            PlanejamentoMapper planejamentoMapper,
            PlanejamentoRecorrenteVinculoService recorrenteVinculoService,
            GeracaoPlanejamentosRecorrentesService geracaoPlanejamentosRecorrentesService
    ) {
        this.planejamentoRepository = planejamentoRepository;
        this.categoriaRepository = categoriaRepository;
        this.periodoFinanceiroService = periodoFinanceiroService;
        this.userService = userService;
        this.planejamentoMapper = planejamentoMapper;
        this.recorrenteVinculoService = recorrenteVinculoService;
        this.geracaoPlanejamentosRecorrentesService = geracaoPlanejamentosRecorrentesService;
    }

    @Transactional(readOnly = true)
    public Page<PlanejamentoResponseDTO> findAll(PlanejamentoFilterCriteria criteria, Pageable pageable) {
        Long userId = SecurityUtils.currentUserId();
        PeriodoFinanceiro periodo = periodoFinanceiroService.resolvePeriodoToTransacao(criteria.getPeriodoId(), userId);

        PlanejamentoFilterCriteria planejamentos = criteria;
        planejamentos.setPeriodoId(periodo.getId());

        Specification<Planejamento> specification = PlanejamentoSpecification.createSpecification(planejamentos, userId);
        return planejamentoRepository.findAll(specification, pageable).map(planejamentoMapper::toResponseDTO);
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

        if (request.transacaoRecorrenteId() != null) {
            String chaveSincronizacao = recorrenteVinculoService.resolverChaveSincronizacao(
                    request.transacaoRecorrenteId(),
                    periodo,
                    userId
            );
            planejamento.setChaveSincronizacao(chaveSincronizacao);
        }

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
    public void deleteAllByPeriodo(Long periodoId) {
        Long userId = SecurityUtils.currentUserId();
        PeriodoFinanceiro periodo = periodoFinanceiroService.resolvePeriodoToTransacao(periodoId, userId);
        planejamentoRepository.deleteAllByPeriodoIdAndUserId(periodo.getId(), userId);
    }

    @Transactional
    public SincronizacaoPlanejamentosResponseDTO sincronizarRecorrentes(Long periodoId) {
        Long userId = SecurityUtils.currentUserId();
        PeriodoFinanceiro periodo = periodoFinanceiroService.resolvePeriodoToTransacao(periodoId, userId);
        return geracaoPlanejamentosRecorrentesService.sincronizar(periodo, userId);
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
}
