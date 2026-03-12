package br.com.budgetflow.features.categorias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "categorias",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_categorias_descricao", columnNames = "descricao")
        }
)
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ClassificacaoCategoria classificacao;
}
