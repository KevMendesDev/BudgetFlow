package br.com.budgetflow.features.auth.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.budgetflow.features.auth.domain.RefreshToken;
import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT refreshToken FROM RefreshToken refreshToken
        JOIN FETCH refreshToken.user
        WHERE refreshToken.tokenHash = :tokenHash
            """)
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    List<RefreshToken> findAllByUserIdOrderByCreatedAtAsc(Long userId);

    @Modifying
    @Query("DELETE FROM RefreshToken refreshToken WHERE refreshToken.expiresAt < :now OR refreshToken.revoked = true")
    int deleteExpiredOrRevoked(@Param("now") OffsetDateTime now);

    @Modifying
    @Query("DELETE FROM RefreshToken refreshToken WHERE refreshToken.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
