package br.com.budgetflow.features.planejamentos.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import br.com.budgetflow.features.planejamentos.domain.Planejamento;
import br.com.budgetflow.features.planejamentos.dto.PlanejamentoResponseDTO;

@Mapper(componentModel = "spring")
public interface PlanejamentoMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "categoria.id", target = "categoriaId")
    @Mapping(source = "categoria.nome", target = "categoriaNome")
    @Mapping(source = "categoria.classificacao", target = "classificacaoCategoria")
    @Mapping(source = "periodo.id", target = "periodoId")
    @Mapping(source = "sincronizado", target = "sincronizado")
    PlanejamentoResponseDTO toResponseDTO(Planejamento entity);
}
