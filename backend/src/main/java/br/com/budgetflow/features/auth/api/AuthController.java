package br.com.budgetflow.features.auth.api;

import br.com.budgetflow.features.auth.dto.*;
import br.com.budgetflow.features.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(
                new MessageResponseDTO(authService.register(request, httpRequest.getRemoteAddr())));
    }

    @PostMapping("/login")
    public ResponseEntity<CurrentUserResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        return ResponseEntity.ok(
                authService.login(request.email(), request.senha(), httpRequest.getRemoteAddr(), response));
    }

    @PostMapping("/email-verification/confirm")
    public ResponseEntity<MessageResponseDTO> confirmEmail(@Valid @RequestBody TokenRequestDTO request) {
        authService.confirmEmail(request.token());
        return ResponseEntity.ok(new MessageResponseDTO("Email confirmado. Você já pode entrar."));
    }

    @PostMapping("/email-verification/resend")
    public ResponseEntity<MessageResponseDTO> resendVerification(@Valid @RequestBody EmailRequestDTO request) {
        return ResponseEntity.ok(new MessageResponseDTO(authService.resendVerification(request.email())));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<MessageResponseDTO> forgotPassword(@Valid @RequestBody EmailRequestDTO request) {
        return ResponseEntity.ok(new MessageResponseDTO(authService.forgotPassword(request.email())));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<MessageResponseDTO> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        authService.resetPassword(request.token(), request.senha());
        return ResponseEntity.ok(new MessageResponseDTO("Senha alterada. Entre novamente."));
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfTokenResponseDTO> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(new CsrfTokenResponseDTO(csrfToken.getToken(), csrfToken.getHeaderName()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = cookieValue(request, "refresh_token");
        authService.refresh(rawRefreshToken, request.getRemoteAddr(), response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Long userId = null;
        if (authentication != null && authentication.getName() != null) {
            try {
                userId = Long.valueOf(authentication.getName());
            } catch (NumberFormatException ignored) {
                // Invalid principals are treated as anonymous logout requests.
            }
        }
        authService.logout(userId, cookieValue(request, "refresh_token"), request, response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponseDTO> me(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(authService.me(Long.valueOf(authentication.getName())));
    }

    private String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
