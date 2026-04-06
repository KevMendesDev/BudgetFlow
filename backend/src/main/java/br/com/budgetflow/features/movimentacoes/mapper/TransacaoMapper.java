package br.com.budgetflow.features.movimentacoes.mapper;

import br.com.budgetflow.features.movimentacoes.domain.Transacao;
import br.com.budgetflow.features.movimentacoes.dto.TransacaoRequestDTO;
import br.com.budgetflow.features.movimentacoes.dto.TransacaoResponseDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface TransacaoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @Mapping(target = "periodo", ignore = true)
    @Mapping(target = "transacaoRecorrente", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Transacao toEntity(TransacaoRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @Mapping(target = "periodo", ignore = true)
    @Mapping(target = "transacaoRecorrente", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(TransacaoRequestDTO dto, @MappingTarget Transacao entity);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "categoria.id", target = "categoriaId")
    @Mapping(source = "categoria.nome", target = "categoriaNome")
    @Mapping(source = "categoria.classificacao", target = "classificacaoCategoria")
    @Mapping(source = "periodo.id", target = "periodoId")
    @Mapping(source = "transacaoRecorrente.id", target = "transacaoRecorrenteId")
    TransacaoResponseDTO toResponseDTO(Transacao entity);
}
