package br.com.budgetflow.features.periodos.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.budgetflow.common.service.RelacionamentoChecker;
import br.com.budgetflow.features.periodos.domain.PeriodoFinanceiro;
import br.com.budgetflow.features.periodos.dto.PeriodoFinanceiroRequestDTO;
import br.com.budgetflow.features.periodos.dto.PeriodoFinanceiroResponseDTO;

@Mapper(componentModel = "spring")
public abstract class PeriodoFinanceiroMapper {
    @Autowired
    protected RelacionamentoChecker relacionamentoChecker;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "dataInicio", ignore = true)
    @Mapping(target = "dataFim", ignore = true)
    public abstract PeriodoFinanceiro toEntity(PeriodoFinanceiroRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "dataInicio", ignore = true)
    @Mapping(target = "dataFim", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updatePeriodoFromDto(PeriodoFinanceiroRequestDTO dto, @MappingTarget PeriodoFinanceiro entity);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(target = "possuiRelacionamentos", ignore = true)
    protected abstract PeriodoFinanceiroResponseDTO toResponseDTOBase(PeriodoFinanceiro periodoFinanceiro);

    public PeriodoFinanceiroResponseDTO toResponseDTO(PeriodoFinanceiro periodoFinanceiro) {
        boolean possuiRelacionamentos = relacionamentoChecker.periodoHasRelationships(periodoFinanceiro.getId(), periodoFinanceiro.getUser().getId());
        return toResponseDTO(periodoFinanceiro, possuiRelacionamentos);
    }

    public PeriodoFinanceiroResponseDTO toResponseDTO(PeriodoFinanceiro periodoFinanceiro, boolean possuiRelacionamentos) {
        PeriodoFinanceiroResponseDTO periodoFinanceiroBase = toResponseDTOBase(periodoFinanceiro);
        return new PeriodoFinanceiroResponseDTO(
            periodoFinanceiroBase.id(), periodoFinanceiroBase.userId(), periodoFinanceiroBase.mes(), periodoFinanceiroBase.ano(),
            periodoFinanceiroBase.dataInicio(), periodoFinanceiroBase.dataFim(), periodoFinanceiroBase.createdAt(), periodoFinanceiroBase.updatedAt(),
            possuiRelacionamentos
        );
    }
}
