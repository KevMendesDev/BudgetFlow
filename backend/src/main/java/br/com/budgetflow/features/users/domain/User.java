package br.com.budgetflow.features.users.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "ix_users_email", columnList = "email"),
                @Index(name = "ix_users_cpf", columnList = "cpf")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_users_cpf", columnNames = "cpf")
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 11)
    private String cpf;

    private String telefone;

    @Column(nullable = false)
    private String senha;

    @Column(nullable = false)
    private String roles = "USER";
}
