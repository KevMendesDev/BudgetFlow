package br.com.budgetflow.features.movimentacoes.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.budgetflow.common.enums.StatusTransacao;
import br.com.budgetflow.common.exceptions.BusinessRuleException;
import br.com.budgetflow.common.exceptions.ResourceNotFoundException;
import br.com.budgetflow.common.utils.DateRangeUtils;
import br.com.budgetflow.features.categorias.domain.Categoria;
import br.com.budgetflow.features.categorias.repository.CategoriaRepository;
import br.com.budgetflow.features.movimentacoes.criteria.TransacaoFilterCriteria;
import br.com.budgetflow.features.movimentacoes.domain.Transacao;
import br.com.budgetflow.features.movimentacoes.domain.TransacaoRecorrente;
import br.com.budgetflow.features.movimentacoes.dto.SincronizacaoRecorrentesResponseDTO;
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

@Service
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final TransacaoRecorrenteService transacaoRecorrenteService;
    private final CategoriaRepository categoriaRepository;
    private final PeriodoFinanceiroService periodoFinanceiroService;
    private final UserService userService;
    private final TransacaoMapper transacaoMapper;
    private final GeracaoTransacoesDoPeriodoService geracaoTransacoesDoPeriodoService;

    public TransacaoService(
            TransacaoRepository transacaoRepository,
            TransacaoRecorrenteService transacaoRecorrenteService,
            CategoriaRepository categoriaRepository,
            PeriodoFinanceiroService periodoFinanceiroService,
            UserService userService,
            TransacaoMapper transacaoMapper,
            GeracaoTransacoesDoPeriodoService geracaoTransacoesDoPeriodoService
    ) {
        this.transacaoRepository = transacaoRepository;
        this.transacaoRecorrenteService = transacaoRecorrenteService;
        this.categoriaRepository = categoriaRepository;
        this.periodoFinanceiroService = periodoFinanceiroService;
        this.userService = userService;
        this.transacaoMapper = transacaoMapper;
        this.geracaoTransacoesDoPeriodoService = geracaoTransacoesDoPeriodoService;
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

        PeriodoFinanceiro periodo = periodoFinanceiroService.resolvePeriodoToTransacao(requestDTO.periodoId(), userId);

        transacaoMapper.updateFromDto(requestDTO, transacao);
        transacao.setPeriodo(periodo);

        this.fillTransacaoData(transacao, requestDTO, userId, id);

        return transacaoMapper.toResponseDTO(transacaoRepository.save(transacao));
    }

    @Transactional
    public SincronizacaoRecorrentesResponseDTO sincronizarRecorrentes(Long periodoId) {
        Long userId = SecurityUtils.currentUserId();
        PeriodoFinanceiro periodo = periodoFinanceiroService.resolvePeriodoToTransacao(periodoId, userId);
        return geracaoTransacoesDoPeriodoService.gerarParaPeriodo(periodo);
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
            transacao.setValor(resolveValorRecorrente(requestDTO, transacaoRecorrente));
            transacao.setTipoMovimentacao(transacaoRecorrente.getTipoMovimentacao());
            transacao.setTipoPagamento(transacaoRecorrente.getTipoPagamento());
            transacao.setStatus(resolveStatus(requestDTO.status(), StatusTransacao.EXECUTADO));
            return;
        }

        this.validateManualTransacaoRequest(requestDTO);

        Categoria categoria = categoriaRepository.findById(requestDTO.categoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));

        if (!categoria.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Categoria não encontrada");
        }

        if (!categoria.getTipoCategoria().name().equals(requestDTO.tipoMovimentacao().name())) {
            throw new BusinessRuleException("O tipo da categoria deve ser igual ao tipo de movimentação");
        }

        transacao.setTransacaoRecorrente(null);
        transacao.setCategoria(categoria);
        transacao.setDescricao(requestDTO.descricao().trim());
        transacao.setValor(requestDTO.valor());
        transacao.setTipoMovimentacao(requestDTO.tipoMovimentacao());
        transacao.setTipoPagamento(requestDTO.tipoPagamento());
        transacao.setStatus(resolveStatus(requestDTO.status(), StatusTransacao.EXECUTADO));
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
        if (currentTransacaoId == null) {
            LocalDate ultimaData = transacaoRepository
                    .findFirstByTransacaoRecorrenteIdAndUserIdOrderByDataDescIdDesc(recorrenteId, userId)
                    .map(Transacao::getData)
                    .orElse(null);

            RecorrenciaUtils.validarIntervaloRecorrencia(
                    transacao.getData(),
                    ultimaData,
                    transacaoRecorrente.getFrequencia()
            );
        } else {
            LocalDate dataAnterior = transacaoRepository
                    .findFirstByTransacaoRecorrenteIdAndUserIdAndIdNotAndDataLessThanEqualOrderByDataDescIdDesc(
                            recorrenteId,
                            userId,
                            currentTransacaoId,
                            transacao.getData()
                    )
                    .map(Transacao::getData)
                    .orElse(null);
            LocalDate dataPosterior = transacaoRepository
                    .findFirstByTransacaoRecorrenteIdAndUserIdAndIdNotAndDataGreaterThanEqualOrderByDataAscIdAsc(
                            recorrenteId,
                            userId,
                            currentTransacaoId,
                            transacao.getData()
                    )
                    .map(Transacao::getData)
                    .orElse(null);

            RecorrenciaUtils.validarIntervaloEntreRecorrencias(
                    transacao.getData(),
                    dataAnterior,
                    dataPosterior,
                    transacaoRecorrente.getFrequencia()
            );
        }

        long parcelasLancadas = currentTransacaoId == null
                ? transacaoRepository.countByTransacaoRecorrenteIdAndUserId(recorrenteId, userId)
                : transacaoRepository.countByTransacaoRecorrenteIdAndUserIdAndIdNot(recorrenteId, userId, currentTransacaoId);

        RecorrenciaUtils.validarLimiteParcelas(transacaoRecorrente.getTotalParcelas(), parcelasLancadas);
    }

    private BigDecimal resolveValorRecorrente(TransacaoRequestDTO requestDTO, TransacaoRecorrente transacaoRecorrente) {
        if (requestDTO.valor() != null) {
            return requestDTO.valor();
        }
        if (transacaoRecorrente.getValor() != null) {
            return transacaoRecorrente.getValor();
        }
        throw new BusinessRuleException("O valor é obrigatório para transações criadas a partir de recorrência sem valor");
    }

    private StatusTransacao resolveStatus(StatusTransacao status, StatusTransacao defaultStatus) {
        return status == null ? defaultStatus : status;
    }

    private void validateManualTransacaoRequest(TransacaoRequestDTO requestDTO) {
        if (requestDTO.categoriaId() == null
                || requestDTO.descricao() == null
                || requestDTO.descricao().isBlank()
                || requestDTO.valor() == null
                || requestDTO.tipoPagamento() == null) {
            throw new BusinessRuleException("Para transação sem recorrência, categoria, descrição, valor, tipo de movimentação e tipo de pagamento são obrigatórios");
        }
    }

    private Transacao findByIdAndUserId(Long id, Long userId) {
        return transacaoRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada"));
    }
}
