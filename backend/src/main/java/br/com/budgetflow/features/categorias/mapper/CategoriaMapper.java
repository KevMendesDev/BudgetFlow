package br.com.budgetflow.features.categorias.mapper;

import br.com.budgetflow.features.categorias.domain.Categoria;
import br.com.budgetflow.features.categorias.dto.CategoriaRequestDTO;
import br.com.budgetflow.features.categorias.dto.CategoriaResponseDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "user", ignore = true)
	Categoria toEntity(CategoriaRequestDTO dto);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "user", ignore = true)
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void updateCategoriaFromDto(CategoriaRequestDTO dto, @MappingTarget Categoria entity);

	@Mapping(source = "user.id", target = "userId")
	CategoriaResponseDTO toResponseDTO(Categoria categoria);
}
