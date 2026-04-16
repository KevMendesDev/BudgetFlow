package br.com.budgetflow.features.movimentacoes.service;

import br.com.budgetflow.features.categorias.domain.Categoria;
import br.com.budgetflow.features.categorias.repository.CategoriaRepository;
import br.com.budgetflow.features.movimentacoes.criteria.TransacaoFilterCriteria;
import br.com.budgetflow.features.movimentacoes.domain.Transacao;
import br.com.budgetflow.features.movimentacoes.domain.TransacaoRecorrente;
import br.com.budgetflow.features.movimentacoes.dto.TransacaoRequestDTO;
import br.com.budgetflow.features.movimentacoes.dto.TransacaoResponseDTO;
import br.com.budgetflow.features.movimentacoes.mapper.TransacaoMapper;
import br.com.budgetflow.features.movimentacoes.repository.TransacaoRepository;
import br.com.budgetflow.features.movimentacoes.repository.specification.TransacaoSpecification;
import br.com.budgetflow.features.periodos.domain.PeriodoFinanceiro;
import br.com.budgetflow.features.periodos.service.PeriodoFinanceiroService;
import br.com.budgetflow.features.users.domain.User;
import br.com.budgetflow.features.users.service.UserService;
import br.com.budgetflow.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final TransacaoRecorrenteService transacaoRecorrenteService;
    private final CategoriaRepository categoriaRepository;
    private final PeriodoFinanceiroService periodoFinanceiroService;
    private final UserService userService;
    private final TransacaoMapper transacaoMapper;

    public TransacaoService(
            TransacaoRepository transacaoRepository,
            TransacaoRecorrenteService transacaoRecorrenteService,
            CategoriaRepository categoriaRepository,
            PeriodoFinanceiroService periodoFinanceiroService,
            UserService userService,
            TransacaoMapper transacaoMapper
    ) {
        this.transacaoRepository = transacaoRepository;
        this.transacaoRecorrenteService = transacaoRecorrenteService;
        this.categoriaRepository = categoriaRepository;
        this.periodoFinanceiroService = periodoFinanceiroService;
        this.userService = userService;
        this.transacaoMapper = transacaoMapper;
    }

    @Transactional
    public TransacaoResponseDTO create(TransacaoRequestDTO requestDTO) {
        Long userId = SecurityUtils.currentUserId();
        User user = userService.findById(userId);

        PeriodoFinanceiro periodo = periodoFinanceiroService.resolvePeriodoToTransacao(requestDTO.periodoId(), userId);
        Transacao transacao = transacaoMapper.toEntity(requestDTO);
        transacao.setUser(user);
        transacao.setPeriodo(periodo);

        this.fillTransacaoData(transacao, requestDTO, userId);

        return transacaoMapper.toResponseDTO(transacaoRepository.save(transacao));
    }

    @Transactional(readOnly = true)
    public Page<TransacaoResponseDTO> findAll(
            TransacaoFilterCriteria criteria,
            Pageable pageable
    ) {
        validateDateRange(criteria.getDataInicio(), criteria.getDataFim());

        Long userId = SecurityUtils.currentUserId();
        Long effectivePeriodoId = periodoFinanceiroService.resolvePeriodoIdForFilterToTransacao(userId, criteria.getPeriodoId());

        Specification<Transacao> specification = TransacaoSpecification.createSpecification(criteria, userId, effectivePeriodoId);

        return transacaoRepository.findAll(specification, pageable)
                .map(transacaoMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public TransacaoResponseDTO findById(Long id) {
        Long userId = SecurityUtils.currentUserId();
        Transacao transacao = findByIdAndUserId(id, userId);
        return transacaoMapper.toResponseDTO(transacao);
    }

    @Transactional
    public TransacaoResponseDTO update(Long id, TransacaoRequestDTO requestDTO) {
        Long userId = SecurityUtils.currentUserId();
        Transacao transacao = findByIdAndUserId(id, userId);

        PeriodoFinanceiro periodo = requestDTO.periodoId() == null
                ? transacao.getPeriodo()
                : periodoFinanceiroService.resolvePeriodoToTransacao(requestDTO.periodoId(), userId);

        transacaoMapper.updateFromDto(requestDTO, transacao);
        transacao.setPeriodo(periodo);

        this.fillTransacaoData(transacao, requestDTO, userId);

        return transacaoMapper.toResponseDTO(transacaoRepository.save(transacao));
    }

    @Transactional
    public void delete(Long id) {
        Long userId = SecurityUtils.currentUserId();
        Transacao transacao = findByIdAndUserId(id, userId);
        transacaoRepository.delete(transacao);
    }

    private void fillTransacaoData(Transacao transacao, TransacaoRequestDTO requestDTO, Long userId) {
        if (requestDTO.transacaoRecorrenteId() != null) {
            TransacaoRecorrente transacaoRecorrente = transacaoRecorrenteService.findEntityByIdAndUser(requestDTO.transacaoRecorrenteId(), userId);
            transacao.setTransacaoRecorrente(transacaoRecorrente);
            transacao.setCategoria(transacaoRecorrente.getCategoria());
            transacao.setDescricao(transacaoRecorrente.getDescricao());
            transacao.setValor(transacaoRecorrente.getValor());
            transacao.setTipoMovimentacao(transacaoRecorrente.getTipoMovimentacao());
            transacao.setTipoPagamento(transacaoRecorrente.getTipoPagamento());
            return;
        }

        this.validateManualTransacaoRequest(requestDTO);

        Categoria categoria = categoriaRepository.findById(requestDTO.categoriaId())
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));

        if (!categoria.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Categoria não encontrada");
        }

        transacao.setTransacaoRecorrente(null);
        transacao.setCategoria(categoria);
        transacao.setDescricao(requestDTO.descricao().trim());
        transacao.setValor(requestDTO.valor());
        transacao.setTipoMovimentacao(requestDTO.tipoMovimentacao());
        transacao.setTipoPagamento(requestDTO.tipoPagamento());
    }

    private void validateManualTransacaoRequest(TransacaoRequestDTO requestDTO) {
        if (requestDTO.categoriaId() == null
                || requestDTO.descricao() == null
                || requestDTO.descricao().isBlank()
                || requestDTO.valor() == null
                || requestDTO.tipoMovimentacao() == null
                || requestDTO.tipoPagamento() == null) {
            throw new IllegalArgumentException("Para transação sem recorrência, categoria, descrição, valor, tipo de movimentação e tipo de pagamento são obrigatórios");
        }
    }

    private Transacao findByIdAndUserId(Long id, Long userId) {
        return transacaoRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada"));
    }

    private void validateDateRange(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio != null && dataFim != null && dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException("A data de fim não pode ser anterior à data de início");
        }
    }
}
