package br.com.budgetflow.features.auth.repository;

import br.com.budgetflow.features.auth.domain.AuthToken;
import br.com.budgetflow.features.auth.domain.AuthTokenType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AuthToken> findByTokenHashAndType(String tokenHash, AuthTokenType type);

    List<AuthToken> findAllByUserIdAndTypeAndUsedAtIsNull(Long userId, AuthTokenType type);

    Optional<AuthToken> findFirstByUserIdAndTypeOrderByCreatedAtDesc(Long userId, AuthTokenType type);
}
