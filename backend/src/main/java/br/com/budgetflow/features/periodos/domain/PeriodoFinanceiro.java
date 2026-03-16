package br.com.budgetflow.features.periodos.domain;

import br.com.budgetflow.features.users.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "periodos_financeiros",
        indexes = {
                @Index(name = "ix_periodos_user_id", columnList = "user_id")
        }
)
public class PeriodoFinanceiro {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,  generator = "sequence_generator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer mes;

    @Column(nullable = false)
    private Integer ano;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
        PeriodoFinanceiro other = (PeriodoFinanceiro) obj;
        if (id == null) {
                if (other.id != null)
                        return false;
        } else if (!id.equals(other.id))
                return false;
        return true;
    }

    @Override
    public String toString() {
        return "PeriodoFinanceiro [id=" + id + ", mes=" + mes + ", ano=" + ano + ", dataInicio="
                + dataInicio + ", dataFim=" + dataFim + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
    }
}
