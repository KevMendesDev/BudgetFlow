package br.com.budgetflow.features.movimentacoes.domain;

import br.com.budgetflow.features.periodos.domain.PeriodoFinanceiro;
import jakarta.persistence.*;
import lombok.AccessLevel;
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
                @Index(name = "ix_transacoes_user_id", columnList = "user_id"),
                @Index(name = "ix_transacoes_periodo_id", columnList = "periodo_id"),
                @Index(name = "ix_transacoes_recorrente_id", columnList = "transacao_recorrente_id")
        }
)
public class Transacao extends Movimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_generator")
    @SequenceGenerator(name = "sequence_generator", sequenceName = "sequence_generator", allocationSize = 1)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periodo_id", nullable = false)
    private PeriodoFinanceiro periodo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transacao_recorrente_id")
    private TransacaoRecorrente transacaoRecorrente;

    @Column(nullable = false)
    private LocalDate data;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
                return true;
        if (obj == null)
                return false;
        if (getClass() != obj.getClass())
                return false;
        Transacao other = (Transacao) obj;
        if (id == null) {
                if (other.id != null)
                        return false;
        } else if (!id.equals(other.id))
                return false;
        return true;
    }

    @Override
    public String toString() {
        return "Transacao [id=" + id + ", periodo=" + periodo + ", transacaoRecorrente=" + transacaoRecorrente + ", data="
                + data + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
    }
}
