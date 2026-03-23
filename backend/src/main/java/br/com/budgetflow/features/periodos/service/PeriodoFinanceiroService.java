package br.com.budgetflow.features.periodos.service;

import br.com.budgetflow.features.periodos.domain.PeriodoFinanceiro;
import br.com.budgetflow.features.periodos.dto.PeriodoFinanceiroRequestDTO;
import br.com.budgetflow.features.periodos.dto.PeriodoFinanceiroResponseDTO;
import br.com.budgetflow.features.periodos.mapper.PeriodoFinanceiroMapper;
import br.com.budgetflow.features.periodos.repository.PeriodoFinanceiroRepository;
import br.com.budgetflow.features.users.domain.User;
import br.com.budgetflow.features.users.service.UserService;
import br.com.budgetflow.security.SecurityUtils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public Page<PeriodoFinanceiroResponseDTO> findAll(Pageable pageable) {
        Long userId = SecurityUtils.currentUserId();

        return periodoFinanceiroRepository.findAllByUserId(userId, pageable)
                .map(periodoFinanceiroMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public PeriodoFinanceiroResponseDTO findById(Long id) {
        PeriodoFinanceiro periodo = periodoFinanceiroRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Período financeiro não encontrado"));
        return periodoFinanceiroMapper.toResponseDTO(periodo);
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

    private void validateDates(PeriodoFinanceiroRequestDTO periodoDTO) {
        if (periodoDTO.dataFim().isBefore(periodoDTO.dataInicio())) {
            throw new IllegalArgumentException("A data de fim não pode ser anterior à data de início");
        }
    }
}
