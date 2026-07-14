package br.com.budgetflow.features.auth.service;

import br.com.budgetflow.common.exceptions.BusinessRuleException;
import br.com.budgetflow.features.auth.domain.AuthToken;
import br.com.budgetflow.features.auth.domain.AuthTokenType;
import br.com.budgetflow.features.auth.repository.AuthTokenRepository;
import br.com.budgetflow.features.users.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthTokenService {

    private final AuthTokenRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthTokenService(AuthTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public String issue(User user, AuthTokenType type, Duration validity) {
        OffsetDateTime now = OffsetDateTime.now();
        repository.findAllByUserIdAndTypeAndUsedAtIsNull(user.getId(), type)
                .forEach(token -> token.setUsedAt(now));

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        AuthToken token = new AuthToken();
        token.setUser(user);
        token.setType(type);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(now.plus(validity));
        repository.save(token);
        return rawToken;
    }

    @Transactional
    public User consume(String rawToken, AuthTokenType type) {
        AuthToken token = repository.findByTokenHashAndType(hash(rawToken), type)
                .orElseThrow(() -> new BusinessRuleException("Token inválido ou expirado"));

        OffsetDateTime now = OffsetDateTime.now();
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) {
            throw new BusinessRuleException("Token inválido ou expirado");
        }

        token.setUsedAt(now);
        return token.getUser();
    }

    @Transactional(readOnly = true)
    public boolean issuedWithin(User user, AuthTokenType type, Duration interval) {
        return repository.findFirstByUserIdAndTypeOrderByCreatedAtDesc(user.getId(), type)
                .map(token -> token.getCreatedAt().isAfter(OffsetDateTime.now().minus(interval)))
                .orElse(false);
    }

    private String hash(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não disponível", exception);
        }
    }
}
