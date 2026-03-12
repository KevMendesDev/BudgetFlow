package br.com.budgetflow.features.planejamentos.domain;

import br.com.budgetflow.features.categorias.domain.Categoria;
import br.com.budgetflow.features.periodos.domain.PeriodoFinanceiro;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "planejamentos",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_planejamentos_periodo_categoria", columnNames = {"periodo_id", "categoria_id"})
        }
)
public class Planejamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periodo_id", nullable = false)
    private PeriodoFinanceiro periodo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(name = "valor_planejado", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorPlanejado;
}
