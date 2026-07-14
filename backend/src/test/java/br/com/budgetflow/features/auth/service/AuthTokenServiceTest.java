package br.com.budgetflow.features.auth.service;

import br.com.budgetflow.common.exceptions.BusinessRuleException;
import br.com.budgetflow.features.auth.domain.AuthToken;
import br.com.budgetflow.features.auth.domain.AuthTokenType;
import br.com.budgetflow.features.auth.repository.AuthTokenRepository;
import br.com.budgetflow.features.users.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

    @Mock private AuthTokenRepository repository;

    @Test
    void issueInvalidatesPreviousTokenAndStoresOnlyHash() {
        AuthToken previous = new AuthToken();
        User user = new User("Usuário", "user@example.com", null, "encoded");
        when(repository.findAllByUserIdAndTypeAndUsedAtIsNull(null, AuthTokenType.EMAIL_VERIFICATION))
                .thenReturn(List.of(previous));

        AuthTokenService service = new AuthTokenService(repository);
        String rawToken = service.issue(user, AuthTokenType.EMAIL_VERIFICATION, Duration.ofHours(24));

        ArgumentCaptor<AuthToken> captor = ArgumentCaptor.forClass(AuthToken.class);
        verify(repository).save(captor.capture());
        assertNotNull(previous.getUsedAt());
        assertNotEquals(rawToken, captor.getValue().getTokenHash());
        assertEquals(64, captor.getValue().getTokenHash().length());
    }

    @Test
    void consumeRejectsExpiredOrUsedToken() {
        AuthToken token = new AuthToken();
        token.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
        when(repository.findByTokenHashAndType(anyString(), eq(AuthTokenType.PASSWORD_RESET)))
                .thenReturn(Optional.of(token));

        AuthTokenService service = new AuthTokenService(repository);

        assertThrows(
                BusinessRuleException.class,
                () -> service.consume("raw-token", AuthTokenType.PASSWORD_RESET));
    }

    @Test
    void consumeMarksValidTokenAsUsed() {
        User user = new User("Usuário", "user@example.com", null, "encoded");
        AuthToken token = new AuthToken();
        token.setUser(user);
        token.setExpiresAt(OffsetDateTime.now().plusHours(1));
        when(repository.findByTokenHashAndType(anyString(), eq(AuthTokenType.PASSWORD_RESET)))
                .thenReturn(Optional.of(token));

        AuthTokenService service = new AuthTokenService(repository);
        User result = service.consume("raw-token", AuthTokenType.PASSWORD_RESET);

        assertSame(user, result);
        assertNotNull(token.getUsedAt());
    }
}
