package br.com.budgetflow.features.movimentacoes.mapper;

import br.com.budgetflow.features.movimentacoes.domain.TransacaoRecorrente;
import br.com.budgetflow.features.movimentacoes.dto.TransacaoRecorrenteRequestDTO;
import br.com.budgetflow.features.movimentacoes.dto.TransacaoRecorrenteResponseDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface TransacaoRecorrenteMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @Mapping(source = "valorParcela", target = "valor")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TransacaoRecorrente toEntity(TransacaoRecorrenteRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @Mapping(source = "valorParcela", target = "valor")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(TransacaoRecorrenteRequestDTO dto, @MappingTarget TransacaoRecorrente entity);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "categoria.id", target = "categoriaId")
    @Mapping(source = "categoria.nome", target = "categoriaNome")
    @Mapping(source = "categoria.classificacao", target = "classificacaoCategoria")
    @Mapping(source = "valor", target = "valorParcela")
    @Mapping(
            target = "valorTotal",
            expression = "java(br.com.budgetflow.features.movimentacoes.service.support.RecorrenciaUtils.calcularValorTotal(entity.getValor(), entity.getTotalParcelas()))"
    )
    TransacaoRecorrenteResponseDTO toResponseDTO(TransacaoRecorrente entity);
}
