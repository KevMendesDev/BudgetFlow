package br.com.budgetflow.features.periodos.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import br.com.budgetflow.features.periodos.domain.PeriodoFinanceiro;
import br.com.budgetflow.features.periodos.dto.PeriodoFinanceiroRequestDTO;
import br.com.budgetflow.features.periodos.dto.PeriodoFinanceiroResponseDTO;

@Mapper(componentModel = "spring")
public interface PeriodoFinanceiroMapper {

    @Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "user", ignore = true)
    PeriodoFinanceiro toEntity(PeriodoFinanceiroRequestDTO dto);

    @Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "user", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updatePeriodoFromDto(PeriodoFinanceiroRequestDTO dto, @MappingTarget PeriodoFinanceiro entity);

    @Mapping(source = "user.id", target = "userId")
    PeriodoFinanceiroResponseDTO toResponseDTO(PeriodoFinanceiro periodoFinanceiro);
}
