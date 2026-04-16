package br.com.budgetflow.features.periodos.service;

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

    public PeriodoFinanceiroService(
            PeriodoFinanceiroRepository periodoFinanceiroRepository,
            UserService userService,
            PeriodoFinanceiroMapper periodoFinanceiroMapper
    ) {
        this.periodoFinanceiroRepository = periodoFinanceiroRepository;
        this.userService = userService;
        this.periodoFinanceiroMapper = periodoFinanceiroMapper;
    }

    @Transactional
    public PeriodoFinanceiroResponseDTO create(PeriodoFinanceiroRequestDTO periodoDTO) {
        this.validateDates(periodoDTO);

        User user = this.userService.findById(SecurityUtils.currentUserId());

        PeriodoFinanceiro periodo = periodoFinanceiroMapper.toEntity(periodoDTO);
        periodo.setUser(user);

        return periodoFinanceiroMapper.toResponseDTO(periodoFinanceiroRepository.save(periodo));
    }

    @Transactional(readOnly = true)
        public Page<PeriodoFinanceiroResponseDTO> findAll(
            Long userId,
            LocalDate dataInicio,
            LocalDate dataFim,
            String search,
            Pageable pageable
        ) {
        this.validateDateRange(dataInicio, dataFim);

        Long currentUserId = SecurityUtils.currentUserId();

        Specification<PeriodoFinanceiro> specification = Specification
            .where(PeriodoFinanceiroSpecification.hasCurrentUserId(currentUserId))
            .and(PeriodoFinanceiroSpecification.hasUserId(userId))
            .and(PeriodoFinanceiroSpecification.hasDataInicioFrom(dataInicio))
            .and(PeriodoFinanceiroSpecification.hasDataFimTo(dataFim))
            .and(PeriodoFinanceiroSpecification.hasSearchTerm(search));

        return periodoFinanceiroRepository.findAll(specification, pageable)
                .map(periodoFinanceiroMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public PeriodoFinanceiro findById(Long id) {
        PeriodoFinanceiro periodo = periodoFinanceiroRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Período financeiro não encontrado"));
        return periodo;
    }

    @Transactional
    public PeriodoFinanceiroResponseDTO update(Long id, PeriodoFinanceiroRequestDTO periodoDTO) {
        this.validateDates(periodoDTO);

        PeriodoFinanceiro periodo = periodoFinanceiroRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Período financeiro não encontrado"));

        User user = this.userService.findById(SecurityUtils.currentUserId());

        periodoFinanceiroMapper.updatePeriodoFromDto(periodoDTO, periodo);
        periodo.setUser(user);

        return periodoFinanceiroMapper.toResponseDTO(periodoFinanceiroRepository.save(periodo));
    }

    @Transactional
    public void delete(Long id) {
        if (!periodoFinanceiroRepository.existsById(id)) {
            throw new IllegalArgumentException("Período financeiro não encontrado");
        }
        periodoFinanceiroRepository.deleteById(id);
    }

    public PeriodoFinanceiro resolvePeriodoToTransacao(Long periodoId, Long userId) {
        if (periodoId == null) {
            return periodoFinanceiroRepository
                    .findFirstByUserIdAndDataInicioLessThanEqualAndDataFimGreaterThanEqual(userId, LocalDate.now(), LocalDate.now())
                    .orElseThrow(() -> new IllegalArgumentException("Nenhum período financeiro atual encontrado para o usuário"));
        }

        PeriodoFinanceiro periodo = this.findById(periodoId);

        if (!periodo.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Período financeiro não encontrado");
        }

        return periodo;
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

    private void validateDates(PeriodoFinanceiroRequestDTO periodoDTO) {
        if (periodoDTO.dataFim().isBefore(periodoDTO.dataInicio())) {
            throw new IllegalArgumentException("A data de fim não pode ser anterior à data de início");
        }
    }

    private void validateDateRange(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio != null && dataFim != null && dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException("A data de fim não pode ser anterior à data de início");
        }
    }
}
