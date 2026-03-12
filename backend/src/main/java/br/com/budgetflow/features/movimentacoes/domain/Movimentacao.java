package br.com.budgetflow.features.movimentacoes.domain;

import br.com.budgetflow.common.enums.TipoMovimentacao;
import br.com.budgetflow.common.enums.TipoPagamento;
import br.com.budgetflow.features.categorias.domain.Categoria;
import br.com.budgetflow.features.users.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@MappedSuperclass
public abstract class Movimentacao {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimentacao", nullable = false, length = 50)
    private TipoMovimentacao tipoMovimentacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pagamento", nullable = false, length = 50)
    private TipoPagamento tipoPagamento;
}
