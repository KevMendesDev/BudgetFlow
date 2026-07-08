package br.com.budgetflow.features.categorias.mapper;

import br.com.budgetflow.features.categorias.domain.Categoria;
import br.com.budgetflow.features.categorias.dto.CategoriaRequestDTO;
import br.com.budgetflow.features.categorias.dto.CategoriaResponseDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public abstract class CategoriaMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    public abstract Categoria toEntity(CategoriaRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updateCategoriaFromDto(CategoriaRequestDTO dto, @MappingTarget Categoria entity);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(target = "possuiRelacionamentos", ignore = true)
    protected abstract CategoriaResponseDTO toResponseDTOBase(Categoria categoria);

    public CategoriaResponseDTO toResponseDTO(Categoria categoria, boolean possuiRelacionamentos) {
        CategoriaResponseDTO categoriaBase = toResponseDTOBase(categoria);
        return new CategoriaResponseDTO(
            categoriaBase.id(), categoriaBase.nome(), categoriaBase.classificacao(), categoriaBase.tipoCategoria(),
            categoriaBase.userId(), categoriaBase.createdAt(), categoriaBase.updatedAt(),
            possuiRelacionamentos
        );
    }
}
