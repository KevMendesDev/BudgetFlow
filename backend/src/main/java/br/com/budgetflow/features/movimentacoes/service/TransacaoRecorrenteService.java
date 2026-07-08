package br.com.budgetflow.features.movimentacoes.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.budgetflow.common.exceptions.BusinessRuleException;
import br.com.budgetflow.common.exceptions.EntityHasRelationshipsException;
import br.com.budgetflow.common.exceptions.ResourceNotFoundException;
import br.com.budgetflow.common.service.RelacionamentoChecker;
import br.com.budgetflow.common.utils.DateRangeUtils;
import br.com.budgetflow.features.categorias.domain.Categoria;
import br.com.budgetflow.features.categorias.service.CategoriaService;
import br.com.budgetflow.features.movimentacoes.criteria.TransacaoRecorrenteFilterCriteria;
import br.com.budgetflow.features.movimentacoes.domain.TransacaoRecorrente;
import br.com.budgetflow.features.movimentacoes.dto.TransacaoRecorrenteRequestDTO;
import br.com.budgetflow.features.movimentacoes.dto.TransacaoRecorrenteResponseDTO;
import br.com.budgetflow.features.movimentacoes.mapper.TransacaoRecorrenteMapper;
import br.com.budgetflow.features.movimentacoes.repository.TransacaoRecorrenteRepository;
import br.com.budgetflow.features.movimentacoes.repository.projection.TransacaoRecorrenteUsageProjection;
import br.com.budgetflow.features.movimentacoes.repository.specification.TransacaoRecorrenteSpecification;
import br.com.budgetflow.features.movimentacoes.service.support.RecorrenciaUtils;
import br.com.budgetflow.features.periodos.service.PeriodoFinanceiroService;
import br.com.budgetflow.features.users.domain.User;
import br.com.budgetflow.features.users.service.UserService;
import br.com.budgetflow.security.SecurityUtils;

@Service
public class TransacaoRecorrenteService {

    private final TransacaoRecorrenteRepository transacaoRecorrenteRepository;
    private final UserService userService;
    private final TransacaoRecorrenteMapper transacaoRecorrenteMapper;
    private final CategoriaService categoriaService;
    private final RelacionamentoChecker relacionamentoChecker;
    private final PeriodoFinanceiroService periodoFinanceiroService;

    public TransacaoRecorrenteService(
            TransacaoRecorrenteRepository transacaoRecorrenteRepository,
            UserService userService,
            TransacaoRecorrenteMapper transacaoRecorrenteMapper,
            CategoriaService categoriaService,
            RelacionamentoChecker relacionamentoChecker,
            PeriodoFinanceiroService periodoFinanceiroService
    ) {
        this.transacaoRecorrenteRepository = transacaoRecorrenteRepository;
        this.userService = userService;
        this.transacaoRecorrenteMapper = transacaoRecorrenteMapper;
        this.categoriaService = categoriaService;
        this.relacionamentoChecker = relacionamentoChecker;
        this.periodoFinanceiroService = periodoFinanceiroService;
    }

    @Transactional
    public TransacaoRecorrenteResponseDTO create(TransacaoRecorrenteRequestDTO requestDTO) {
        Long userId = SecurityUtils.currentUserId();
        User user = userService.findById(userId);
        Categoria categoria = categoriaService.findEntityByIdAndUser(requestDTO.categoriaId(), userId);

        DateRangeUtils.validateRange(requestDTO.dataInicio(), requestDTO.dataFim());
        validateCategoriaTipo(categoria, requestDTO.tipoMovimentacao().name());
        validateValorParcela(requestDTO.valorParcela());

        TransacaoRecorrente transacaoRecorrente = transacaoRecorrenteMapper.toEntity(requestDTO);
        transacaoRecorrente.setUser(user);
        transacaoRecorrente.setCategoria(categoria);
        transacaoRecorrente.setDescricao(requestDTO.descricao().trim());
        transacaoRecorrente.setValor(requestDTO.valorParcela());
        transacaoRecorrente.setDataFim(resolveDataFim(requestDTO));

        return transacaoRecorrenteMapper.toResponseDTO(
                transacaoRecorrenteRepository.save(transacaoRecorrente),
                false
        );
    }

    @Transactional(readOnly = true)
    public Page<TransacaoRecorrenteResponseDTO> findAll(
            TransacaoRecorrenteFilterCriteria criteria,
            Pageable pageable
    ) {
        DateRangeUtils.validateRange(criteria.getDataInicio(), criteria.getDataFim());

        Long userId = SecurityUtils.currentUserId();

        Specification<TransacaoRecorrente> specification = TransacaoRecorrenteSpecification
                .createSpecification(criteria, userId);

        Page<TransacaoRecorrente> transacoesPage = transacaoRecorrenteRepository.findAll(specification, pageable);
        List<Long> transacaoRecorrenteIds = transacoesPage.getContent().stream()
                .map(TransacaoRecorrente::getId)
                .toList();

        if (transacaoRecorrenteIds.isEmpty()) {
            return transacoesPage.map(transacaoRecorrente -> transacaoRecorrenteMapper.toResponseDTO(transacaoRecorrente, false));
        }

        Set<Long> transacoesComRelacionamentos = relacionamentoChecker.findTransacaoRecorrenteIdsWithRelationships(
                transacaoRecorrenteIds,
                userId
        );

        return transacoesPage.map(transacaoRecorrente ->
                transacaoRecorrenteMapper.toResponseDTO(
                        transacaoRecorrente,
                        transacoesComRelacionamentos.contains(transacaoRecorrente.getId())
                ));
    }

    @Transactional(readOnly = true)
    public TransacaoRecorrenteResponseDTO findById(Long id) {
        Long userId = SecurityUtils.currentUserId();
        TransacaoRecorrente transacaoRecorrente = findByIdAndUserId(id, userId);
        boolean possuiRelacionamentos = relacionamentoChecker.transacaoRecorrenteHasRelationships(id, userId);
        return transacaoRecorrenteMapper.toResponseDTO(transacaoRecorrente, possuiRelacionamentos);
    }

    @Transactional(readOnly = true)
    public List<TransacaoRecorrenteResponseDTO> findDisponiveisParaLancamento(Long periodoId, LocalDate data) {
        Long userId = SecurityUtils.currentUserId();
        periodoFinanceiroService.resolvePeriodoToTransacao(periodoId, userId);

        return transacaoRecorrenteRepository.findUsageByUserId(userId).stream()
                .filter(usage -> podeSerLancada(usage, data))
                .map(usage -> transacaoRecorrenteMapper.toResponseDTO(
                        usage.getRecorrente(),
                        usage.getParcelasLancadas() > 0
                ))
                .toList();
    }

    @Transactional
    public TransacaoRecorrenteResponseDTO update(Long id, TransacaoRecorrenteRequestDTO requestDTO) {
        Long userId = SecurityUtils.currentUserId();
        TransacaoRecorrente transacaoRecorrente = findByIdAndUserId(id, userId);
        Categoria categoria = categoriaService.findEntityByIdAndUser(requestDTO.categoriaId(), userId);

        DateRangeUtils.validateRange(requestDTO.dataInicio(), requestDTO.dataFim());
        validateCategoriaTipo(categoria, requestDTO.tipoMovimentacao().name());
        validateValorParcela(requestDTO.valorParcela());

        transacaoRecorrenteMapper.updateFromDto(requestDTO, transacaoRecorrente);
        transacaoRecorrente.setCategoria(categoria);
        transacaoRecorrente.setDescricao(requestDTO.descricao().trim());
        transacaoRecorrente.setValor(requestDTO.valorParcela());
        transacaoRecorrente.setDataFim(resolveDataFim(requestDTO));

        TransacaoRecorrente updatedTransacaoRecorrente = transacaoRecorrenteRepository.save(transacaoRecorrente);
        boolean possuiRelacionamentos = relacionamentoChecker.transacaoRecorrenteHasRelationships(id, userId);
        return transacaoRecorrenteMapper.toResponseDTO(updatedTransacaoRecorrente, possuiRelacionamentos);
    }

    @Transactional
    public void delete(Long id) {
        Long userId = SecurityUtils.currentUserId();
        TransacaoRecorrente transacaoRecorrente = findByIdAndUserId(id, userId);

        if (relacionamentoChecker.transacaoRecorrenteHasRelationships(id, userId)) {
            throw new EntityHasRelationshipsException(
                    "A transação recorrente \"" + transacaoRecorrente.getDescricao() + "\" não pode ser excluída pois está vinculada a uma ou mais transações"
            );
        }

        transacaoRecorrenteRepository.delete(transacaoRecorrente);
    }

    @Transactional(readOnly = true)
    public TransacaoRecorrente findEntityByIdAndUser(Long id, Long userId) {
        return findByIdAndUserId(id, userId);
    }

    private LocalDate resolveDataFim(TransacaoRecorrenteRequestDTO requestDTO) {
        if (requestDTO.totalParcelas() != null) {
            return RecorrenciaUtils.calcularDataFim(requestDTO.dataInicio(), requestDTO.frequencia(), requestDTO.totalParcelas());
        }
        return requestDTO.dataFim();
    }

    private void validateCategoriaTipo(Categoria categoria, String tipoMovimentacao) {
        if (!categoria.getTipoCategoria().name().equals(tipoMovimentacao)) {
            throw new BusinessRuleException("O tipo da categoria deve ser igual ao tipo de movimentação");
        }
    }

    private void validateValorParcela(java.math.BigDecimal valorParcela) {
        if (valorParcela != null && valorParcela.signum() <= 0) {
            throw new BusinessRuleException("O valor da parcela deve ser maior que zero");
        }
    }

    private boolean podeSerLancada(TransacaoRecorrenteUsageProjection usage, LocalDate data) {
        TransacaoRecorrente recorrente = usage.getRecorrente();
        if (data.isBefore(recorrente.getDataInicio())) {
            return false;
        }

        if (recorrente.getDataFim() != null && data.isAfter(recorrente.getDataFim())) {
            return false;
        }

        long parcelasLancadas = usage.getParcelasLancadas();
        if (recorrente.getTotalParcelas() != null && parcelasLancadas >= recorrente.getTotalParcelas()) {
            return false;
        }

        LocalDate ultimaData = usage.getUltimaData();

        if (ultimaData == null) {
            return true;
        }

        LocalDate proximaDataMinima = RecorrenciaUtils.calcularProximaDataMinima(ultimaData, recorrente.getFrequencia());
        return !data.isBefore(proximaDataMinima);
    }

    private TransacaoRecorrente findByIdAndUserId(Long id, Long userId) {
        return transacaoRecorrenteRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transação recorrente não encontrada"));
    }
}
