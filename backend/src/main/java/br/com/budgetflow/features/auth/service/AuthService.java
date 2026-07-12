package br.com.budgetflow.features.auth.service;

import br.com.budgetflow.common.exceptions.ConflictException;
import br.com.budgetflow.common.exceptions.UnauthorizedException;
import br.com.budgetflow.features.auth.domain.AuthTokenType;
import br.com.budgetflow.features.auth.domain.RefreshToken;
import br.com.budgetflow.features.auth.dto.CurrentUserResponseDTO;
import br.com.budgetflow.features.auth.dto.RegisterRequestDTO;
import br.com.budgetflow.features.auth.repository.RefreshTokenRepository;
import br.com.budgetflow.features.users.domain.Role;
import br.com.budgetflow.features.users.domain.User;
import br.com.budgetflow.features.users.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class AuthService {

    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());
    private static final Duration VERIFICATION_VALIDITY = Duration.ofHours(24);
    private static final Duration RESET_VALIDITY = Duration.ofHours(1);
    private static final Duration EMAIL_REQUEST_INTERVAL = Duration.ofMinutes(1);
    private static final String GENERIC_EMAIL_MESSAGE =
            "Se a conta estiver apta, enviaremos as instruções por email.";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthCookieService cookieService;
    private final AuthTokenService authTokenService;
    private final ResendEmailService emailService;
    private final AuthThrottleService throttleService;
    private final long accessTokenMinutes;
    private final long refreshTokenDays;
    private final int maxSessionsPerUser;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthCookieService cookieService,
            AuthTokenService authTokenService,
            ResendEmailService emailService,
            AuthThrottleService throttleService,
            @Value("${app.security.jwt.access-token-minutes}") long accessTokenMinutes,
            @Value("${app.security.jwt.refresh-token-days}") long refreshTokenDays,
            @Value("${app.security.max-sessions-per-user:5}") int maxSessionsPerUser) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cookieService = cookieService;
        this.authTokenService = authTokenService;
        this.emailService = emailService;
        this.throttleService = throttleService;
        this.accessTokenMinutes = accessTokenMinutes;
        this.refreshTokenDays = refreshTokenDays;
        this.maxSessionsPerUser = maxSessionsPerUser;
    }

    @Transactional
    public String register(RegisterRequestDTO request, String clientAddress) {
        String throttleKey = "register:" + clientAddress;
        throttleService.check(throttleKey);
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email já cadastrado");
        }

        User user = new User(
                request.nome().trim(),
                normalizedEmail,
                request.telefone() == null || request.telefone().isBlank() ? null : request.telefone().trim(),
                passwordEncoder.encode(request.senha()));
        userRepository.save(user);
        sendVerificationEmail(user);
        return "Conta criada. Verifique seu email para liberar o acesso.";
    }

    @Transactional
    public CurrentUserResponseDTO login(
            String email,
            String senha,
            String clientAddress,
            HttpServletResponse response
    ) {
        String normalizedEmail = normalizeEmail(email);
        String throttleKey = "login:" + clientAddress + ":" + normalizedEmail;
        throttleService.check(throttleKey);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("E-mail ou senha inválidos"));

        if (user.getSenha() == null || !passwordEncoder.matches(senha, user.getSenha())) {
            throw new UnauthorizedException("Email ou senha inválidos");
        }
        if (user.getEmailVerifiedAt() == null) {
            throw new UnauthorizedException("Confirme seu email antes de entrar", "EMAIL_NOT_VERIFIED");
        }

        CurrentUserResponseDTO result = issueTokensAndReturn(user, response);
        throttleService.reset(throttleKey);
        return result;
    }

    @Transactional
    public CurrentUserResponseDTO loginWithGoogle(OidcUser oidcUser, HttpServletResponse response) {
        if (!Boolean.TRUE.equals(oidcUser.getEmailVerified())) {
            throw new UnauthorizedException("Email do Google não verificado");
        }

        String subject = oidcUser.getSubject();
        String email = normalizeEmail(oidcUser.getEmail());
        if (subject == null || subject.isBlank() || email == null || email.isBlank()) {
            throw new UnauthorizedException("Conta Google sem identificação válida");
        }

        User user = userRepository.findByGoogleSubject(subject)
                .orElseGet(() -> userRepository.findByEmail(email)
                        .map(existingUser -> {
                            if (existingUser.getGoogleSubject() != null
                                    && !existingUser.getGoogleSubject().equals(subject)) {
                                throw new UnauthorizedException("Email já vinculado a outra conta Google");
                            }
                            existingUser.setGoogleSubject(subject);
                            return existingUser;
                        })
                        .orElseGet(() -> {
                            String name = oidcUser.getFullName();
                            if (name == null || name.isBlank()) {
                                name = email;
                            }
                            User newUser = new User(name.trim(), email, null, null);
                            newUser.setGoogleSubject(subject);
                            return newUser;
                        }));

        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
        return issueTokensAndReturn(user, response);
    }

    @Transactional
    public void confirmEmail(String token) {
        User user = authTokenService.consume(token, AuthTokenType.EMAIL_VERIFICATION);
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(LocalDateTime.now());
        }
    }

    @Transactional
    public String resendVerification(String email) {
        userRepository.findByEmail(normalizeEmail(email))
                .filter(user -> user.getEmailVerifiedAt() == null)
                .filter(user -> user.getSenha() != null)
                .filter(user -> !authTokenService.issuedWithin(
                        user, AuthTokenType.EMAIL_VERIFICATION, EMAIL_REQUEST_INTERVAL))
                .ifPresent(this::sendVerificationEmail);
        return GENERIC_EMAIL_MESSAGE;
    }

    @Transactional
    public String forgotPassword(String email) {
        userRepository.findByEmail(normalizeEmail(email))
                .filter(user -> user.getEmailVerifiedAt() != null)
                .filter(user -> user.getSenha() != null)
                .filter(user -> !authTokenService.issuedWithin(
                        user, AuthTokenType.PASSWORD_RESET, EMAIL_REQUEST_INTERVAL))
                .ifPresent(this::sendPasswordResetEmail);
        return GENERIC_EMAIL_MESSAGE;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = authTokenService.consume(token, AuthTokenType.PASSWORD_RESET);
        user.setSenha(passwordEncoder.encode(newPassword));
        refreshTokenRepository.deleteAllByUserId(user.getId());
    }

    @Transactional
    public void refresh(String rawRefreshToken, String clientAddress, HttpServletResponse response) {
        throttleService.check("refresh:" + clientAddress);
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token inválido");
        }

        String hash = hashToken(rawRefreshToken);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHashForUpdate(hash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido"));
        if (refreshToken.isRevoked()) {
            refreshTokenRepository.deleteAllByUserId(refreshToken.getUser().getId());
            throw new UnauthorizedException("Refresh token revogado");
        }
        if (refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("Refresh token expirado");
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        User user = refreshToken.getUser();
        issueTokensAndReturn(user, response);
        throttleService.reset("refresh:" + clientAddress);
    }

    @Transactional
    public void logout(
            Long userId,
            String rawRefreshToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (userId == null && rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            userId = refreshTokenRepository.findByTokenHash(hashToken(rawRefreshToken))
                    .map(token -> token.getUser().getId())
                    .orElse(null);
        }
        if (userId != null) {
            refreshTokenRepository.deleteAllByUserId(userId);
        }
        cookieService.clearCookies(response);
        invalidateHttpSession(request);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponseDTO me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Usuário não encontrado"));
        return new CurrentUserResponseDTO(user.getId(), user.getNome(), user.getEmail(), rolesToStrings(user.getRoles()));
    }

    private void sendVerificationEmail(User user) {
        String token = authTokenService.issue(user, AuthTokenType.EMAIL_VERIFICATION, VERIFICATION_VALIDITY);
        try {
            emailService.sendVerification(user, token);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Falha ao enviar verificação para usuário " + user.getId(), exception);
        }
    }

    private void sendPasswordResetEmail(User user) {
        String token = authTokenService.issue(user, AuthTokenType.PASSWORD_RESET, RESET_VALIDITY);
        try {
            emailService.sendPasswordReset(user, token);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Falha ao enviar recuperação para usuário " + user.getId(), exception);
        }
    }

    private CurrentUserResponseDTO issueTokensAndReturn(User user, HttpServletResponse response) {
        enforceSessionLimit(user.getId());
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRoles());
        String rawRefreshToken = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(rawRefreshToken));
        refreshToken.setExpiresAt(OffsetDateTime.now().plusDays(refreshTokenDays));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        cookieService.setAccessTokenCookie(response, accessToken, (int) (accessTokenMinutes * 60));
        cookieService.setRefreshTokenCookie(response, rawRefreshToken, (int) (refreshTokenDays * 24 * 60 * 60));

        return new CurrentUserResponseDTO(
                user.getId(), user.getNome(), user.getEmail(), rolesToStrings(user.getRoles()));
    }

    private List<String> rolesToStrings(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return roles.stream().map(Role::name).toList();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private void enforceSessionLimit(Long userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findAllByUserIdOrderByCreatedAtAsc(userId);
        int tokensToDelete = tokens.size() - maxSessionsPerUser + 1;
        if (tokensToDelete > 0) {
            refreshTokenRepository.deleteAll(tokens.subList(0, tokensToDelete));
        }
    }

    private String hashToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não disponível", exception);
        }
    }

    private void invalidateHttpSession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
