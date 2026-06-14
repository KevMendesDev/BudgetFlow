package br.com.budgetflow.features.auth.service;

import br.com.budgetflow.common.exceptions.ConflictException;
import br.com.budgetflow.common.exceptions.UnauthorizedException;
import br.com.budgetflow.features.auth.domain.RefreshToken;
import br.com.budgetflow.features.auth.dto.CurrentUserResponseDTO;
import br.com.budgetflow.features.auth.dto.RegisterRequestDTO;
import br.com.budgetflow.features.auth.repository.RefreshTokenRepository;
import br.com.budgetflow.features.users.domain.Role;
import br.com.budgetflow.features.users.domain.User;
import br.com.budgetflow.features.users.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthCookieService cookieService;
    private final long accessTokenMinutes;
    private final long refreshTokenDays;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthCookieService cookieService,
            @Value("${app.security.jwt.access-token-minutes}") long accessTokenMinutes,
            @Value("${app.security.jwt.refresh-token-days}") long refreshTokenDays) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cookieService = cookieService;
        this.accessTokenMinutes = accessTokenMinutes;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public CurrentUserResponseDTO register(RegisterRequestDTO request, HttpServletResponse response) {
        String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("E-mail já cadastrado");
        }

        User user = new User(
            request.nome().trim(),
            normalizedEmail,
            request.telefone() == null || request.telefone().isBlank() ? null : request.telefone().trim(),
            passwordEncoder.encode(request.senha())
        );
        userRepository.save(user);

        return login(normalizedEmail, request.senha(), response);
    }

    @Transactional
    public CurrentUserResponseDTO login(String email, String senha, HttpServletResponse response) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new UnauthorizedException("E-mail ou senha inválidos"));

        if (user.getSenha() == null || !passwordEncoder.matches(senha, user.getSenha())) {
            throw new UnauthorizedException("E-mail ou senha inválidos");
        }

        return issueTokensAndReturn(user, response);
    }

    @Transactional
    public CurrentUserResponseDTO loginWithGoogle(OidcUser oidcUser, HttpServletResponse response) {
        if (!Boolean.TRUE.equals(oidcUser.getEmailVerified())) {
            throw new UnauthorizedException("E-mail do Google não verificado");
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
                                throw new UnauthorizedException("E-mail já vinculado a outra conta Google");
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

        userRepository.save(user);
        return issueTokensAndReturn(user, response);
    }

    @Transactional
    public void refresh(String rawRefreshToken, HttpServletResponse response) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token inválido");
        }

        String hash = hashToken(rawRefreshToken);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido"));

        if (refreshToken.isRevoked()) {
            throw new UnauthorizedException("Refresh token revogado");
        }
        if (refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("Refresh token expirado");
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        User user = refreshToken.getUser();
        issueTokensAndReturn(user, response);
    }

    @Transactional
    public void logout(Long userId, HttpServletResponse response) {
        if (userId != null) {
            refreshTokenRepository.deleteAllByUserId(userId);
        }
        cookieService.clearCookies(response);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponseDTO me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Usuário não encontrado"));

        List<String> roles = rolesToStrings(user.getRoles());

        return new CurrentUserResponseDTO(user.getId(), user.getNome(), user.getEmail(), roles);
    }

    private String normalizeEmail(String email) {
        if (email == null) return null;
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private CurrentUserResponseDTO issueTokensAndReturn(User user, HttpServletResponse response) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRoles());

        String rawRefreshToken = UUID.randomUUID().toString();
        String hash = hashToken(rawRefreshToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash);
        refreshToken.setExpiresAt(OffsetDateTime.now().plusDays(refreshTokenDays));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        int accessMaxAge = (int) (accessTokenMinutes * 60);
        int refreshMaxAge = (int) (refreshTokenDays * 24 * 60 * 60);

        cookieService.setAccessTokenCookie(response, accessToken, accessMaxAge);
        cookieService.setRefreshTokenCookie(response, rawRefreshToken, refreshMaxAge);

        List<String> roles = rolesToStrings(user.getRoles());

        return new CurrentUserResponseDTO(user.getId(), user.getNome(), user.getEmail(), roles);
    }

    private List<String> rolesToStrings(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of(Role.USER.name());
        }

        return roles.stream()
                .map(Role::name)
                .toList();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
