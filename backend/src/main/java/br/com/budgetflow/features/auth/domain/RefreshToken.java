package br.com.budgetflow.features.auth.domain;

import br.com.budgetflow.features.users.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "refresh_tokens",
        indexes = {
                @Index(name = "ix_refresh_tokens_user_id", columnList = "user_id"),
                @Index(name = "ix_refresh_tokens_hash", columnList = "token_hash")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_refresh_tokens_hash", columnNames = "token_hash")
        }
)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;
}
