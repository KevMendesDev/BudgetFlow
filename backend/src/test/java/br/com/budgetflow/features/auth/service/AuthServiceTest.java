package br.com.budgetflow.features.auth.service;

import br.com.budgetflow.common.exceptions.UnauthorizedException;
import br.com.budgetflow.features.auth.domain.AuthTokenType;
import br.com.budgetflow.features.auth.dto.RegisterRequestDTO;
import br.com.budgetflow.features.auth.repository.RefreshTokenRepository;
import br.com.budgetflow.features.users.domain.User;
import br.com.budgetflow.features.users.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthCookieService cookieService;
    @Mock private AuthTokenService authTokenService;
    @Mock private BrevoEmailService emailService;
    @Mock private HttpServletResponse response;
    @Mock private OidcUser oidcUser;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                cookieService,
                authTokenService,
                emailService,
                15,
                30);
    }

    @Test
    void registerCreatesPendingUserAndSendsVerificationWithoutCookies() {
        var request = new RegisterRequestDTO("Usuário", " USER@EXAMPLE.COM ", null, "Secret@123");
        when(passwordEncoder.encode(request.senha())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authTokenService.issue(any(), eq(AuthTokenType.EMAIL_VERIFICATION), eq(Duration.ofHours(24))))
                .thenReturn("raw-token");

        String message = authService.register(request);

        assertTrue(message.contains("Verifique"));
        verify(emailService).sendVerification(any(User.class), eq("raw-token"));
        verifyNoInteractions(cookieService);
    }

    @Test
    void loginRejectsUnverifiedAccountWithStructuredCode() {
        User user = new User("Usuário", "user@example.com", null, "encoded");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login("user@example.com", "secret", response));

        assertEquals("EMAIL_NOT_VERIFIED", exception.getCode());
        verifyNoInteractions(cookieService);
    }

    @Test
    void loginVerifiedAccountIssuesCookies() {
        User user = verifiedLocalUser();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access-token");

        var result = authService.login("  USER@EXAMPLE.COM ", "secret", response);

        assertEquals("user@example.com", result.email());
        verify(cookieService).setAccessTokenCookie(response, "access-token", 900);
        verify(cookieService).setRefreshTokenCookie(any(), any(), anyInt());
    }

    @Test
    void confirmEmailMarksUserAsVerified() {
        User user = new User("Usuário", "user@example.com", null, "encoded");
        when(authTokenService.consume("token", AuthTokenType.EMAIL_VERIFICATION)).thenReturn(user);

        authService.confirmEmail("token");

        assertNotNull(user.getEmailVerifiedAt());
    }

    @Test
    void resendWithinOneMinuteDoesNotSendAnotherEmail() {
        User user = new User("Usuário", "user@example.com", null, "encoded");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(authTokenService.issuedWithin(
                user, AuthTokenType.EMAIL_VERIFICATION, Duration.ofMinutes(1))).thenReturn(true);

        authService.resendVerification("user@example.com");

        verify(authTokenService, never()).issue(any(), any(), any());
        verifyNoInteractions(emailService);
    }

    @Test
    void forgotPasswordDoesNotRevealMissingAccount() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        String result = authService.forgotPassword("missing@example.com");

        assertTrue(result.contains("conta estiver apta"));
        verifyNoInteractions(authTokenService, emailService);
    }

    @Test
    void resetPasswordChangesPasswordAndRevokesSessions() {
        User user = verifiedLocalUser();
        when(authTokenService.consume("token", AuthTokenType.PASSWORD_RESET)).thenReturn(user);
        when(passwordEncoder.encode("NewSecret@123")).thenReturn("new-encoded");

        authService.resetPassword("token", "NewSecret@123");

        assertEquals("new-encoded", user.getSenha());
        verify(refreshTokenRepository).deleteAllByUserId(user.getId());
    }

    @Test
    void googleLoginCreatesVerifiedUser() {
        when(oidcUser.getEmailVerified()).thenReturn(true);
        when(oidcUser.getSubject()).thenReturn("google-subject");
        when(oidcUser.getEmail()).thenReturn("USER@EXAMPLE.COM");
        when(oidcUser.getFullName()).thenReturn("Usuário Google");
        when(userRepository.findByGoogleSubject("google-subject")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access-token");

        var result = authService.loginWithGoogle(oidcUser, response);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("user@example.com", result.email());
        assertNotNull(captor.getValue().getEmailVerifiedAt());
    }

    private User verifiedLocalUser() {
        User user = new User("Usuário", "user@example.com", null, "encoded");
        user.setEmailVerifiedAt(LocalDateTime.now());
        return user;
    }
}
