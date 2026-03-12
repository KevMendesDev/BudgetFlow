package br.com.budgetflow.features.movimentacoes.domain;

import br.com.budgetflow.features.periodos.domain.PeriodoFinanceiro;
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
        name = "transacoes",
        indexes = {
                @Index(name = "ix_transacoes_usuario_id", columnList = "usuario_id"),
                @Index(name = "ix_transacoes_periodo_id", columnList = "periodo_id"),
                @Index(name = "ix_transacoes_recorrente_id", columnList = "transacao_recorrente_id")
        }
)
public class Transacao extends Movimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periodo_id", nullable = false)
    private PeriodoFinanceiro periodo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transacao_recorrente_id")
    private TransacaoRecorrente transacaoRecorrente;

    @Column(nullable = false)
    private LocalDate data;
}
