package br.com.budgetflow.features.movimentacoes.mapper;

import br.com.budgetflow.common.service.RelacionamentoChecker;
import br.com.budgetflow.features.movimentacoes.domain.TransacaoRecorrente;
import br.com.budgetflow.features.movimentacoes.dto.TransacaoRecorrenteRequestDTO;
import br.com.budgetflow.features.movimentacoes.dto.TransacaoRecorrenteResponseDTO;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class TransacaoRecorrenteMapper {
    @Autowired
    protected RelacionamentoChecker relacionamentoChecker;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @Mapping(source = "valorParcela", target = "valor")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract TransacaoRecorrente toEntity(TransacaoRecorrenteRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @Mapping(source = "valorParcela", target = "valor")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updateFromDto(TransacaoRecorrenteRequestDTO dto, @MappingTarget TransacaoRecorrente entity);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "categoria.id", target = "categoriaId")
    @Mapping(source = "categoria.nome", target = "categoriaNome")
    @Mapping(source = "categoria.classificacao", target = "classificacaoCategoria")
    @Mapping(source = "valor", target = "valorParcela")
    @Mapping(
            target = "valorTotal",
            expression = "java(br.com.budgetflow.features.movimentacoes.service.support.RecorrenciaUtils.calcularValorTotal(entity.getValor(), entity.getTotalParcelas()))"
    )
    @Mapping(target = "possuiRelacionamentos", ignore = true)
    protected abstract TransacaoRecorrenteResponseDTO toResponseDTOBase(TransacaoRecorrente entity);

    public TransacaoRecorrenteResponseDTO toResponseDTO(TransacaoRecorrente entity) {
        boolean possuiRelacionamentos = relacionamentoChecker.transacaoRecorrenteHasRelationships(entity.getId(), entity.getUser().getId());
        return toResponseDTO(entity, possuiRelacionamentos);
    }

    public TransacaoRecorrenteResponseDTO toResponseDTO(TransacaoRecorrente entity, boolean possuiRelacionamentos) {
        TransacaoRecorrenteResponseDTO transacaoBase = toResponseDTOBase(entity);
        return new TransacaoRecorrenteResponseDTO(
                transacaoBase.id(), transacaoBase.userId(), transacaoBase.categoriaId(), transacaoBase.categoriaNome(),
                transacaoBase.classificacaoCategoria(), transacaoBase.descricao(), transacaoBase.valorParcela(), transacaoBase.valorTotal(),
                transacaoBase.tipoMovimentacao(), transacaoBase.tipoPagamento(), transacaoBase.frequencia(),
                transacaoBase.dataInicio(), transacaoBase.dataFim(), transacaoBase.totalParcelas(),
                transacaoBase.createdAt(), transacaoBase.updatedAt(), possuiRelacionamentos
        );
    }
}
