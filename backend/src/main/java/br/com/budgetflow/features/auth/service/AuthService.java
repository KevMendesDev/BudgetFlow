package br.com.budgetflow.features.auth.service;

import br.com.budgetflow.common.exceptions.UnauthorizedException;
import br.com.budgetflow.features.auth.domain.RefreshToken;
import br.com.budgetflow.features.auth.dto.CurrentUserResponseDTO;
import br.com.budgetflow.features.auth.dto.RegisterRequestDTO;
import br.com.budgetflow.features.auth.repository.RefreshTokenRepository;
import br.com.budgetflow.features.users.domain.User;
import br.com.budgetflow.features.users.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthCookieService cookieService;
    private final long refreshTokenDays;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthCookieService cookieService,
            @Value("${app.security.jwt.refresh-token-days}") long refreshTokenDays) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cookieService = cookieService;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public CurrentUserResponseDTO register(RegisterRequestDTO request, HttpServletResponse response) {
        String cpf = normalizeCpf(request.cpf());
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByCpf(cpf)) {
            throw new IllegalArgumentException("CPF já cadastrado");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }

        User user = new User(
            request.nome().trim(),
            normalizedEmail,
            cpf,
            request.telefone() == null || request.telefone().isBlank() ? null : request.telefone().trim(),
            passwordEncoder.encode(request.senha())
        );
        userRepository.save(user);

        return login(cpf, request.senha(), response);
    }

    @Transactional
    public CurrentUserResponseDTO login(String cpf, String senha, HttpServletResponse response) {
        String cpfNormalizado = normalizeCpf(cpf);

        User user = userRepository.findByCpf(cpfNormalizado)
                .orElseThrow(() -> new UnauthorizedException("CPF ou senha inválidos"));

        if (!passwordEncoder.matches(senha, user.getSenha())) {
            throw new UnauthorizedException("CPF ou senha inválidos");
        }

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

    public CurrentUserResponseDTO me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Usuário não encontrado"));

        List<String> roles = user.getRoles() == null || user.getRoles().isBlank()
                ? List.of("USER")
                : List.of(user.getRoles().split("\\s*,\\s*"));

        return new CurrentUserResponseDTO(user.getId(), user.getNome(), user.getEmail(), user.getCpf(), roles);
    }

    private String normalizeCpf(String cpf) {
        if (cpf == null) return null;
        return cpf.replaceAll("\\D", "");
    }

    private CurrentUserResponseDTO issueTokensAndReturn(User user, HttpServletResponse response) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getCpf(), user.getRoles());

        String rawRefreshToken = UUID.randomUUID().toString();
        String hash = hashToken(rawRefreshToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash);
        refreshToken.setExpiresAt(OffsetDateTime.now().plusDays(refreshTokenDays));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        int accessMaxAge = 15 * 60;
        int refreshMaxAge = (int) (refreshTokenDays * 24 * 60 * 60);

        cookieService.setAccessTokenCookie(response, accessToken, accessMaxAge);
        cookieService.setRefreshTokenCookie(response, rawRefreshToken, refreshMaxAge);

        List<String> roles = user.getRoles() == null || user.getRoles().isBlank()
                ? List.of("USER")
                : List.of(user.getRoles().split("\\s*,\\s*"));

        return new CurrentUserResponseDTO(user.getId(), user.getNome(), user.getEmail(), user.getCpf(), roles);
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
