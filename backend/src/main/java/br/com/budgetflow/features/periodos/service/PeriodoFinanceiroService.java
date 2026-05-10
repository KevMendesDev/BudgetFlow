package br.com.budgetflow.features.periodos.service;

import br.com.budgetflow.common.exceptions.EntityHasRelationshipsException;
import br.com.budgetflow.common.exceptions.ResourceNotFoundException;
import br.com.budgetflow.common.service.RelacionamentoChecker;
import br.com.budgetflow.common.utils.DateRangeUtils;
import br.com.budgetflow.features.periodos.domain.PeriodoFinanceiro;
import br.com.budgetflow.features.periodos.dto.PeriodoFinanceiroRequestDTO;
import br.com.budgetflow.features.periodos.dto.PeriodoFinanceiroResponseDTO;
import br.com.budgetflow.features.periodos.mapper.PeriodoFinanceiroMapper;
import br.com.budgetflow.features.periodos.repository.PeriodoFinanceiroRepository;
import br.com.budgetflow.features.periodos.repository.specification.PeriodoFinanceiroSpecification;
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
public class PeriodoFinanceiroService {

    private final PeriodoFinanceiroRepository periodoFinanceiroRepository;
    private final UserService userService;
    private final PeriodoFinanceiroMapper periodoFinanceiroMapper;
    private final RelacionamentoChecker relacionamentoChecker;

    public PeriodoFinanceiroService(
            PeriodoFinanceiroRepository periodoFinanceiroRepository,
            UserService userService,
            PeriodoFinanceiroMapper periodoFinanceiroMapper,
            RelacionamentoChecker relacionamentoChecker
    ) {
        this.periodoFinanceiroRepository = periodoFinanceiroRepository;
        this.userService = userService;
        this.periodoFinanceiroMapper = periodoFinanceiroMapper;
        this.relacionamentoChecker = relacionamentoChecker;
    }

    @Transactional
    public PeriodoFinanceiroResponseDTO create(PeriodoFinanceiroRequestDTO periodoDTO) {
        DateRangeUtils.validateRange(periodoDTO.dataInicio(), periodoDTO.dataFim());

        User user = this.userService.findById(SecurityUtils.currentUserId());

        PeriodoFinanceiro periodo = periodoFinanceiroMapper.toEntity(periodoDTO);
        periodo.setUser(user);

        return periodoFinanceiroMapper.toResponseDTO(periodoFinanceiroRepository.save(periodo));
    }

    @Transactional(readOnly = true)
    public Page<PeriodoFinanceiroResponseDTO> findAll(
            LocalDate dataInicio,
            LocalDate dataFim,
            String search,
            Pageable pageable
    ) {
        DateRangeUtils.validateRange(dataInicio, dataFim);

        Long currentUserId = SecurityUtils.currentUserId();

        Specification<PeriodoFinanceiro> specification = Specification
                .where(PeriodoFinanceiroSpecification.hasCurrentUserId(currentUserId))
                .and(PeriodoFinanceiroSpecification.hasDataInicioFrom(dataInicio))
                .and(PeriodoFinanceiroSpecification.hasDataFimTo(dataFim))
                .and(PeriodoFinanceiroSpecification.hasSearchTerm(search));

        return periodoFinanceiroRepository.findAll(specification, pageable)
                .map(periodoFinanceiroMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public PeriodoFinanceiro findById(Long id) {
        Long userId = SecurityUtils.currentUserId();
        return periodoFinanceiroRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Período financeiro não encontrado"));
    }

    @Transactional
    public PeriodoFinanceiroResponseDTO update(Long id, PeriodoFinanceiroRequestDTO periodoDTO) {
        DateRangeUtils.validateRange(periodoDTO.dataInicio(), periodoDTO.dataFim());

        Long userId = SecurityUtils.currentUserId();
        PeriodoFinanceiro periodo = periodoFinanceiroRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Período financeiro não encontrado"));

        User user = this.userService.findById(userId);

        periodoFinanceiroMapper.updatePeriodoFromDto(periodoDTO, periodo);
        periodo.setUser(user);

        return periodoFinanceiroMapper.toResponseDTO(periodoFinanceiroRepository.save(periodo));
    }

    @Transactional
    public void delete(Long id) {
        Long userId = SecurityUtils.currentUserId();
        PeriodoFinanceiro periodo = periodoFinanceiroRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Período financeiro não encontrado"));

        if (relacionamentoChecker.periodoHasRelationships(id, userId)) {
            throw new EntityHasRelationshipsException(
                    "O período financeiro \"" + periodo.getDataInicio() + " - " + periodo.getDataFim() + "\" não pode ser excluído pois está vinculado a uma ou mais transações"
            );
        }

        periodoFinanceiroRepository.delete(periodo);
    }

    public PeriodoFinanceiro resolvePeriodoToTransacao(Long periodoId, Long userId) {
        if (periodoId == null) {
            return periodoFinanceiroRepository
                    .findFirstByUserIdAndDataInicioLessThanEqualAndDataFimGreaterThanEqual(userId, LocalDate.now(), LocalDate.now())
                    .orElseThrow(() -> new ResourceNotFoundException("Nenhum período financeiro atual encontrado para o usuário"));
        }

        return periodoFinanceiroRepository.findByIdAndUserId(periodoId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Período financeiro não encontrado"));
    }

    public Long resolvePeriodoIdForFilterToTransacao(Long userId, Long periodoId) {
        if (periodoId != null) {
            return this.resolvePeriodoToTransacao(periodoId, userId).getId();
        }

        return periodoFinanceiroRepository
                .findFirstByUserIdAndDataInicioLessThanEqualAndDataFimGreaterThanEqual(userId, LocalDate.now(), LocalDate.now())
                .map(PeriodoFinanceiro::getId)
                .orElse(null);
    }
}
