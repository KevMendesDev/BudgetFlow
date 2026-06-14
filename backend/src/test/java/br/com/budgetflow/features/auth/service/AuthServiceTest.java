package br.com.budgetflow.features.auth.service;

import br.com.budgetflow.common.exceptions.UnauthorizedException;
import br.com.budgetflow.features.auth.repository.RefreshTokenRepository;
import br.com.budgetflow.features.users.domain.User;
import br.com.budgetflow.features.users.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthCookieService cookieService;
    @Mock
    private HttpServletResponse response;
    @Mock
    private OidcUser oidcUser;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                cookieService,
                15,
                30);
    }

    @Test
    void loginNormalizesEmailAndIssuesCookies() {
        User user = new User("Usuário", "user@example.com", null, "encoded");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access-token");

        var result = authService.login("  USER@EXAMPLE.COM ", "secret", response);

        assertEquals("user@example.com", result.email());
        verify(cookieService).setAccessTokenCookie(response, "access-token", 900);
        verify(cookieService).setRefreshTokenCookie(any(), any(), anyInt());
    }

    @Test
    void loginRejectsGoogleOnlyAccount() {
        User user = new User("Usuário", "user@example.com", null, null);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThrows(
                UnauthorizedException.class,
                () -> authService.login("user@example.com", "secret", response));

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void googleLoginCreatesUser() {
        mockVerifiedGoogleUser("google-subject", "USER@EXAMPLE.COM", "Usuário Google");
        when(userRepository.findByGoogleSubject("google-subject")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access-token");

        var result = authService.loginWithGoogle(oidcUser, response);

        assertEquals("user@example.com", result.email());
        assertEquals("Usuário Google", result.nome());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void googleLoginLinksExistingUserByEmail() {
        User user = new User("Usuário", "user@example.com", null, "encoded");
        when(oidcUser.getEmailVerified()).thenReturn(true);
        when(oidcUser.getSubject()).thenReturn("google-subject");
        when(oidcUser.getEmail()).thenReturn("user@example.com");
        when(userRepository.findByGoogleSubject("google-subject")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access-token");

        authService.loginWithGoogle(oidcUser, response);

        assertEquals("google-subject", user.getGoogleSubject());
    }

    @Test
    void googleLoginRejectsUnverifiedEmail() {
        when(oidcUser.getEmailVerified()).thenReturn(false);

        assertThrows(
                UnauthorizedException.class,
                () -> authService.loginWithGoogle(oidcUser, response));

        verify(userRepository, never()).save(any());
    }

    private void mockVerifiedGoogleUser(String subject, String email, String name) {
        when(oidcUser.getEmailVerified()).thenReturn(true);
        when(oidcUser.getSubject()).thenReturn(subject);
        when(oidcUser.getEmail()).thenReturn(email);
        when(oidcUser.getFullName()).thenReturn(name);
    }
}
