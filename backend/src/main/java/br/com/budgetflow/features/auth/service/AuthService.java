package br.com.budgetflow.features.auth.service;

import br.com.budgetflow.common.exceptions.UnauthorizedException;
import br.com.budgetflow.features.auth.domain.RefreshToken;
import br.com.budgetflow.features.auth.dto.LoginRequest;
import br.com.budgetflow.features.auth.dto.MeResponse;
import br.com.budgetflow.features.auth.dto.RegisterRequest;
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

    private final UserRepository userRepo;
    private final RefreshTokenRepository refreshTokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthCookieService cookieService;
    private final long refreshTokenDays;

    public AuthService(
            UserRepository userRepo,
            RefreshTokenRepository refreshTokenRepo,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthCookieService cookieService,
            @Value("${security.jwt.refresh-token-days}") long refreshTokenDays) {
        this.userRepo = userRepo;
        this.refreshTokenRepo = refreshTokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cookieService = cookieService;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public MeResponse register(RegisterRequest req, HttpServletResponse response) {
        String cpf = normalizarCpf(req.cpf());
        String emailNormalizado = req.email().trim().toLowerCase();

        if (userRepo.existsByCpf(cpf)) {
            throw new IllegalArgumentException("CPF já cadastrado");
        }
        if (userRepo.existsByEmail(emailNormalizado)) {
            throw new IllegalArgumentException("Email já cadastrado");
        }

        User u = new User();
        u.setNome(req.nome().trim());
        u.setEmail(emailNormalizado);
        u.setCpf(cpf);
        u.setTelefone(req.telefone() == null || req.telefone().isBlank() ? null : req.telefone().trim());
        u.setSenha(passwordEncoder.encode(req.senha()));
        u.setRoles("USER");

        userRepo.save(u);

        return loginComCpf(cpf, req.senha(), response);
    }

    @Transactional
    public MeResponse loginComCpf(String cpf, String senha, HttpServletResponse response) {
        String cpfNormalizado = normalizarCpf(cpf);

        User user = userRepo.findByCpf(cpfNormalizado)
                .orElseThrow(() -> new UnauthorizedException("CPF ou senha inválidos"));

        if (!passwordEncoder.matches(senha, user.getSenha())) {
            throw new UnauthorizedException("CPF ou senha inválidos");
        }

        return emitirTokensERetornar(user, response);
    }

    @Transactional
    public void refresh(String rawRefreshToken, HttpServletResponse response) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token inválido");
        }

        String hash = hashToken(rawRefreshToken);
        RefreshToken refreshToken = refreshTokenRepo.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido"));

        if (refreshToken.isRevoked()) {
            throw new UnauthorizedException("Refresh token revogado");
        }
        if (refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("Refresh token expirado");
        }

        // Rotate: revoke old
        refreshToken.setRevoked(true);
        refreshTokenRepo.save(refreshToken);

        User user = refreshToken.getUser();
        emitirTokensERetornar(user, response);
    }

    @Transactional
    public void logout(Long userId, HttpServletResponse response) {
        if (userId != null) {
            refreshTokenRepo.deleteAllByUserId(userId);
        }
        cookieService.clearCookies(response);
    }

    private MeResponse emitirTokensERetornar(User user, HttpServletResponse response) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getCpf(), user.getRoles());

        String rawRefreshToken = UUID.randomUUID().toString();
        String hash = hashToken(rawRefreshToken);

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(hash);
        rt.setExpiresAt(OffsetDateTime.now().plusDays(refreshTokenDays));
        rt.setRevoked(false);
        refreshTokenRepo.save(rt);

        int accessMaxAge = 15 * 60;
        int refreshMaxAge = (int) (refreshTokenDays * 24 * 60 * 60);

        cookieService.setAccessTokenCookie(response, accessToken, accessMaxAge);
        cookieService.setRefreshTokenCookie(response, rawRefreshToken, refreshMaxAge);

        List<String> roles = user.getRoles() == null || user.getRoles().isBlank()
                ? List.of("USER")
                : List.of(user.getRoles().split("\\s*,\\s*"));

        return new MeResponse(user.getId(), user.getNome(), user.getEmail(), user.getCpf(), roles);
    }

    private String normalizarCpf(String cpf) {
        if (cpf == null) return null;
        return cpf.replaceAll("\\D", "");
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
