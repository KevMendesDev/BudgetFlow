package br.com.budgetflow.features.movimentacoes.domain;

import br.com.budgetflow.common.enums.Frequencia;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "transacoes_recorrentes",
        indexes = {
                @Index(name = "ix_transacoes_recorrentes_usuario_id", columnList = "usuario_id")
        }
)
public class TransacaoRecorrente extends Movimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Frequencia frequencia;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(name = "total_parcelas")
    private Integer totalParcelas;
}
