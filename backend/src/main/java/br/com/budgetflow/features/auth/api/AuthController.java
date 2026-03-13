package br.com.budgetflow.features.auth.api;

import br.com.budgetflow.features.auth.dto.LoginRequest;
import br.com.budgetflow.features.auth.dto.MeResponse;
import br.com.budgetflow.common.exceptions.UnauthorizedException;
import br.com.budgetflow.features.auth.dto.RegisterRequest;
import br.com.budgetflow.features.auth.service.AuthService;
import br.com.budgetflow.features.users.domain.User;
import br.com.budgetflow.features.users.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<MeResponse> register(@Valid @RequestBody RegisterRequest req, HttpServletResponse response) {
        return ResponseEntity.ok(authService.register(req, response));
    }

    @PostMapping("/login")
    public ResponseEntity<MeResponse> login(@Valid @RequestBody LoginRequest req, HttpServletResponse response) {
        return ResponseEntity.ok(authService.loginComCpf(req.cpf(), req.senha(), response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refresh = cookieValue(request, "refresh_token");
        authService.refresh(refresh, response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication, HttpServletResponse response) {
        Long userId = null;
        if (authentication != null && authentication.getName() != null) {
            try {
                userId = Long.valueOf(authentication.getName());
            } catch (NumberFormatException ignored) {
            }
        }
        authService.logout(userId, response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = Long.valueOf(authentication.getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Sessão inválida"));

        List<String> roles = user.getRoles() == null || user.getRoles().isBlank()
                ? List.of("USER")
                : List.of(user.getRoles().split("\\s*,\\s*"));

        return ResponseEntity.ok(new MeResponse(
                user.getId(), user.getNome(), user.getEmail(), user.getCpf(), roles));
    }

    private String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}
