package br.com.budgetflow.features.auth.api;

import br.com.budgetflow.features.auth.dto.LoginRequestDTO;
import br.com.budgetflow.features.auth.dto.CurrentUserResponseDTO;
import br.com.budgetflow.features.auth.dto.RegisterRequestDTO;
import br.com.budgetflow.features.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<CurrentUserResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request, HttpServletResponse response) {
        CurrentUserResponseDTO createdUser = authService.register(request, response);
        return ResponseEntity.ok(createdUser);
    }

    @PostMapping("/login")
    public ResponseEntity<CurrentUserResponseDTO> login(@Valid @RequestBody LoginRequestDTO request, HttpServletResponse response) {
        CurrentUserResponseDTO loginUser = authService.login(request.cpf(), request.senha(), response);
        return ResponseEntity.ok(loginUser);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = cookieValue(request, "refresh_token");
        authService.refresh(rawRefreshToken, response);
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
    public ResponseEntity<CurrentUserResponseDTO> me(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }        
        CurrentUserResponseDTO userResponse = authService.me(Long.valueOf(authentication.getName()));

        return ResponseEntity.ok(userResponse);
    }

    private String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}
