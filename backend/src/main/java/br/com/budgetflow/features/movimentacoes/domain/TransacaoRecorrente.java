package br.com.budgetflow.features.movimentacoes.domain;

import br.com.budgetflow.common.enums.Frequencia;
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
        name = "transacoes_recorrentes",
        indexes = {
                @Index(name = "ix_transacoes_recorrentes_user_id", columnList = "user_id")
        }
)
public class TransacaoRecorrente extends Movimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_generator")
    @SequenceGenerator(name = "sequence_generator", sequenceName = "sequence_generator", allocationSize = 1)
    @Setter(AccessLevel.NONE)
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
        TransacaoRecorrente other = (TransacaoRecorrente) obj;
        if (id == null) {
                if (other.id != null)
                        return false;
        } else if (!id.equals(other.id))
                return false;
        return true;
    }

    @Override
    public String toString() {
        return "TransacaoRecorrente [id=" + id + ", frequencia=" + frequencia + ", dataInicio=" + dataInicio + ", dataFim="
                + dataFim + ", totalParcelas=" + totalParcelas + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
    }
}
