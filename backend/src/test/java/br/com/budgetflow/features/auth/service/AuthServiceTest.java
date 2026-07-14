package br.com.budgetflow.features.auth.service;

import br.com.budgetflow.common.exceptions.UnauthorizedException;
import br.com.budgetflow.features.auth.domain.AuthTokenType;
import br.com.budgetflow.features.auth.domain.RefreshToken;
import br.com.budgetflow.features.auth.dto.RegisterRequestDTO;
import br.com.budgetflow.features.auth.repository.RefreshTokenRepository;
import br.com.budgetflow.features.users.domain.User;
import br.com.budgetflow.features.users.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
    private AuthThrottleService throttleService;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpServletRequest request;
    @Mock
    private OidcUser oidcUser;
    @Mock 
    private AuthTokenService authTokenService;
    @Mock 
    private ResendEmailService emailService;

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
                throttleService,
                15,
                30,
                5);
    }

    @Test
    void registerCreatesPendingUserAndSendsVerificationWithoutCookies() {
        var request = new RegisterRequestDTO("Usuario", " USER@EXAMPLE.COM ", null, "Secret@123");
        when(passwordEncoder.encode(request.senha())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authTokenService.issue(any(), eq(AuthTokenType.EMAIL_VERIFICATION), eq(Duration.ofHours(24))))
                .thenReturn("raw-token");

        String message = authService.register(request, "127.0.0.1");

        assertTrue(message.contains("Verifique"));
        verify(throttleService).check("register:127.0.0.1");
        verify(emailService).sendVerification(any(User.class), eq("raw-token"));
        verifyNoInteractions(cookieService);
    }

    @Test
    void loginRejectsUnverifiedAccountWithStructuredCode() {
        User user = new User("Usuario", "user@example.com", null, "encoded");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login("user@example.com", "secret", "127.0.0.1", response));

        assertEquals("EMAIL_NOT_VERIFIED", exception.getCode());
        verify(throttleService).check("login:127.0.0.1:user@example.com");
        verifyNoInteractions(cookieService);
    }

    @Test
    void loginVerifiedAccountIssuesCookiesAndResetsThrottle() {
        User user = verifiedLocalUser();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);
        when(refreshTokenRepository.findAllByUserIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access-token");

        var result = authService.login("  USER@EXAMPLE.COM ", "secret", "127.0.0.1", response);

        assertEquals("user@example.com", result.email());
        verify(throttleService).reset("login:127.0.0.1:user@example.com");
        verify(cookieService).setAccessTokenCookie(response, "access-token", 900);
        verify(cookieService).setRefreshTokenCookie(eq(response), anyString(), anyInt());
    }

    @Test
    void loginRejectsGoogleOnlyAccount() {
        User user = new User("Usuario", "user@example.com", null, null);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThrows(
                UnauthorizedException.class,
                () -> authService.login("user@example.com", "secret", "127.0.0.1", response));

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void confirmEmailMarksUserAsVerified() {
        User user = new User("Usuario", "user@example.com", null, "encoded");
        when(authTokenService.consume("token", AuthTokenType.EMAIL_VERIFICATION)).thenReturn(user);

        authService.confirmEmail("token");

        assertNotNull(user.getEmailVerifiedAt());
    }

    @Test
    void resendWithinOneMinuteDoesNotSendAnotherEmail() {
        User user = new User("Usuario", "user@example.com", null, "encoded");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(authTokenService.issuedWithin(
                user, AuthTokenType.EMAIL_VERIFICATION, Duration.ofMinutes(1))).thenReturn(true);

        authService.resendVerification("user@example.com", "127.0.0.1");

        verify(throttleService).check("email-resend:127.0.0.1");
        verify(authTokenService, never()).issue(any(), any(), any());
        verifyNoInteractions(emailService);
    }

    @Test
    void forgotPasswordDoesNotRevealMissingAccount() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        String result = authService.forgotPassword("missing@example.com", "127.0.0.1");

        assertTrue(result.contains("conta estiver apta"));
        verify(throttleService).check("password-forgot:127.0.0.1");
        verifyNoInteractions(authTokenService, emailService);
    }

    @Test
    void resetPasswordChangesPasswordAndRevokesSessions() {
        User user = verifiedLocalUser();
        when(authTokenService.consume("token", AuthTokenType.PASSWORD_RESET)).thenReturn(user);
        when(passwordEncoder.encode("NewSecret@123")).thenReturn("new-encoded");

        authService.resetPassword("token", "NewSecret@123");

        assertEquals("new-encoded", user.getSenha());
        verify(refreshTokenRepository).deleteAllByUserId(1L);
    }

    @Test
    void googleLoginCreatesVerifiedUser() {
        mockVerifiedGoogleUser("google-subject", "USER@EXAMPLE.COM", "Usuario Google");
        when(userRepository.findByGoogleSubject("google-subject")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });
        when(refreshTokenRepository.findAllByUserIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access-token");

        var result = authService.loginWithGoogle(oidcUser, response);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("user@example.com", result.email());
        assertNotNull(captor.getValue().getEmailVerifiedAt());
    }

    @Test
    void googleLoginLinksExistingUserByEmail() {
        User user = verifiedLocalUser();
        when(oidcUser.getEmailVerified()).thenReturn(true);
        when(oidcUser.getSubject()).thenReturn("google-subject");
        when(oidcUser.getEmail()).thenReturn("user@example.com");
        when(userRepository.findByGoogleSubject("google-subject")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(refreshTokenRepository.findAllByUserIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
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

    @Test
    void refreshLocksTokenBeforeRotatingIt() {
        User user = mock(User.class);
        RefreshToken refreshToken = mock(RefreshToken.class);
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(refreshToken));
        when(refreshToken.isRevoked()).thenReturn(false);
        when(refreshToken.getExpiresAt()).thenReturn(OffsetDateTime.now().plusDays(1));
        when(refreshToken.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(1L);
        when(refreshTokenRepository.findAllByUserIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access-token");

        authService.refresh("refresh-token", "127.0.0.1", response);

        verify(throttleService).check("refresh:127.0.0.1");
        verify(throttleService).reset("refresh:127.0.0.1");
        verify(refreshToken).setRevoked(true);
        verify(refreshTokenRepository).findByTokenHashForUpdate(anyString());
    }

    @Test
    void logoutUsesRefreshCookieWhenAccessTokenIsUnavailable() {
        User user = mock(User.class);
        RefreshToken refreshToken = mock(RefreshToken.class);
        HttpSession session = mock(HttpSession.class);

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));
        when(refreshToken.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(1L);
        when(request.getSession(false)).thenReturn(session);

        authService.logout(null, "refresh-token", request, response);

        verify(refreshTokenRepository).deleteAllByUserId(1L);
        verify(cookieService).clearCookies(response);
        verify(session).invalidate();
    }

    private User verifiedLocalUser() {
        User user = new User("Usuario", "user@example.com", null, "encoded");
        ReflectionTestUtils.setField(user, "id", 1L);
        user.setEmailVerifiedAt(LocalDateTime.now());
        return user;
    }

    private void mockVerifiedGoogleUser(String subject, String email, String name) {
        when(oidcUser.getEmailVerified()).thenReturn(true);
        when(oidcUser.getSubject()).thenReturn(subject);
        when(oidcUser.getEmail()).thenReturn(email);
        when(oidcUser.getFullName()).thenReturn(name);
    }
}
