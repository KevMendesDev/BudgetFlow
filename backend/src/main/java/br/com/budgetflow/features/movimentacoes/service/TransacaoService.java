package br.com.budgetflow.features.movimentacoes.service;

import br.com.budgetflow.common.exceptions.BusinessRuleException;
import br.com.budgetflow.common.exceptions.ResourceNotFoundException;
import br.com.budgetflow.common.utils.DateRangeUtils;
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
import br.com.budgetflow.features.movimentacoes.service.support.RecorrenciaUtils;
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

        this.fillTransacaoData(transacao, requestDTO, userId, null);

        return transacaoMapper.toResponseDTO(transacaoRepository.save(transacao));
    }

    @Transactional(readOnly = true)
    public Page<TransacaoResponseDTO> findAll(
            TransacaoFilterCriteria criteria,
            Pageable pageable
    ) {
        DateRangeUtils.validateRange(criteria.getDataInicio(), criteria.getDataFim());

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

        this.fillTransacaoData(transacao, requestDTO, userId, id);

        return transacaoMapper.toResponseDTO(transacaoRepository.save(transacao));
    }

    @Transactional
    public void delete(Long id) {
        Long userId = SecurityUtils.currentUserId();
        Transacao transacao = findByIdAndUserId(id, userId);
        transacaoRepository.delete(transacao);
    }

    private void fillTransacaoData(Transacao transacao, TransacaoRequestDTO requestDTO, Long userId, Long currentTransacaoId) {
        if (requestDTO.transacaoRecorrenteId() != null) {
            TransacaoRecorrente transacaoRecorrente = transacaoRecorrenteService.findEntityByIdAndUser(requestDTO.transacaoRecorrenteId(), userId);
            validateTransacaoRecorrenteConstraints(transacaoRecorrente, transacao, userId, currentTransacaoId);
            transacao.setTransacaoRecorrente(transacaoRecorrente);
            transacao.setCategoria(transacaoRecorrente.getCategoria());
            transacao.setDescricao(transacaoRecorrente.getDescricao());
            transacao.setTipoMovimentacao(transacaoRecorrente.getTipoMovimentacao());
            transacao.setTipoPagamento(transacaoRecorrente.getTipoPagamento());
            return;
        }

        this.validateManualTransacaoRequest(requestDTO);

        Categoria categoria = categoriaRepository.findById(requestDTO.categoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));

        if (!categoria.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Categoria não encontrada");
        }

        transacao.setTransacaoRecorrente(null);
        transacao.setCategoria(categoria);
        transacao.setDescricao(requestDTO.descricao().trim());
        transacao.setValor(requestDTO.valor());
        transacao.setTipoMovimentacao(requestDTO.tipoMovimentacao());
        transacao.setTipoPagamento(requestDTO.tipoPagamento());
    }

    private void validateTransacaoRecorrenteConstraints(
            TransacaoRecorrente transacaoRecorrente,
            Transacao transacao,
            Long userId,
            Long currentTransacaoId
    ) {
        RecorrenciaUtils.validarDataTransacaoNaRecorrencia(
                transacao.getData(),
                transacaoRecorrente.getDataInicio(),
                transacaoRecorrente.getDataFim()
        );

        Long recorrenteId = transacaoRecorrente.getId();
        Long periodoId = transacao.getPeriodo().getId();

        boolean jaExisteNoPeriodo = currentTransacaoId == null
                ? transacaoRepository.existsByTransacaoRecorrenteIdAndPeriodoIdAndUserId(recorrenteId, periodoId, userId)
                : transacaoRepository.existsByTransacaoRecorrenteIdAndPeriodoIdAndUserIdAndIdNot(recorrenteId, periodoId, userId, currentTransacaoId);

        RecorrenciaUtils.validarRecorrenciaUnicaNoPeriodo(jaExisteNoPeriodo);

        long parcelasLancadas = currentTransacaoId == null
                ? transacaoRepository.countByTransacaoRecorrenteIdAndUserId(recorrenteId, userId)
                : transacaoRepository.countByTransacaoRecorrenteIdAndUserIdAndIdNot(recorrenteId, userId, currentTransacaoId);

        RecorrenciaUtils.validarLimiteParcelas(transacaoRecorrente.getTotalParcelas(), parcelasLancadas);
    }

    private void validateManualTransacaoRequest(TransacaoRequestDTO requestDTO) {
        if (requestDTO.categoriaId() == null
                || requestDTO.descricao() == null
                || requestDTO.descricao().isBlank()
                || requestDTO.valor() == null
                || requestDTO.tipoMovimentacao() == null
                || requestDTO.tipoPagamento() == null) {
            throw new BusinessRuleException("Para transação sem recorrência, categoria, descrição, valor, tipo de movimentação e tipo de pagamento são obrigatórios");
        }
    }

    private Transacao findByIdAndUserId(Long id, Long userId) {
        return transacaoRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada"));
    }
}
