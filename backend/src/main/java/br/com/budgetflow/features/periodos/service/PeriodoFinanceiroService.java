package br.com.budgetflow.features.periodos.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.budgetflow.common.exceptions.ConflictException;
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
        Long userId = SecurityUtils.currentUserId();
        User user = this.userService.findById(userId);
        LocalDate dataInicio = buildDataInicio(periodoDTO);
        LocalDate dataFim = buildDataFim(periodoDTO);
        validateDuplicidade(userId, dataInicio, null);

        PeriodoFinanceiro periodo = periodoFinanceiroMapper.toEntity(periodoDTO);
        periodo.setUser(user);
        periodo.setDataInicio(dataInicio);
        periodo.setDataFim(dataFim);

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

        Page<PeriodoFinanceiro> periodosPage = periodoFinanceiroRepository.findAll(specification, pageable);
        List<Long> periodoIds = periodosPage.getContent().stream()
                .map(PeriodoFinanceiro::getId)
                .toList();

        if (periodoIds.isEmpty()) {
            return periodosPage.map(periodoFinanceiro -> periodoFinanceiroMapper.toResponseDTO(periodoFinanceiro, false));
        }

        Set<Long> periodosComRelacionamentos = relacionamentoChecker.findPeriodoIdsWithRelationships(periodoIds, currentUserId);

        return periodosPage.map(periodoFinanceiro ->
                periodoFinanceiroMapper.toResponseDTO(
                        periodoFinanceiro,
                        periodosComRelacionamentos.contains(periodoFinanceiro.getId())
                ));
    }

    @Transactional(readOnly = true)
    public PeriodoFinanceiro findById(Long id) {
        Long userId = SecurityUtils.currentUserId();
        return periodoFinanceiroRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Período financeiro não encontrado"));
    }

    @Transactional
    public PeriodoFinanceiroResponseDTO update(Long id, PeriodoFinanceiroRequestDTO periodoDTO) {
        Long userId = SecurityUtils.currentUserId();
        PeriodoFinanceiro periodo = periodoFinanceiroRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Período financeiro não encontrado"));
        LocalDate dataInicio = buildDataInicio(periodoDTO);
        LocalDate dataFim = buildDataFim(periodoDTO);
        validateDuplicidade(userId, dataInicio, id);

        User user = this.userService.findById(userId);

        periodoFinanceiroMapper.updatePeriodoFromDto(periodoDTO, periodo);
        periodo.setUser(user);
        periodo.setDataInicio(dataInicio);
        periodo.setDataFim(dataFim);

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

    private LocalDate buildDataInicio(PeriodoFinanceiroRequestDTO periodoDTO) {
        return YearMonth.of(periodoDTO.ano(), periodoDTO.mes()).atDay(1);
    }

    private LocalDate buildDataFim(PeriodoFinanceiroRequestDTO periodoDTO) {
        return YearMonth.of(periodoDTO.ano(), periodoDTO.mes()).atEndOfMonth();
    }

    private void validateDuplicidade(Long userId, LocalDate dataInicio, Long currentId) {
        boolean duplicado = currentId == null
                ? periodoFinanceiroRepository.existsByUserIdAndDataInicio(userId, dataInicio)
                : periodoFinanceiroRepository.existsByUserIdAndDataInicioAndIdNot(userId, dataInicio, currentId);

        if (duplicado) {
            throw new ConflictException("Já existe um período financeiro para este mês e ano");
        }
    }
}
