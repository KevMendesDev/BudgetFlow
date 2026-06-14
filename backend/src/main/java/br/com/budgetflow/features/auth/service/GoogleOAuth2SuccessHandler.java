package br.com.budgetflow.features.auth.service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final String frontendUrl;

    public GoogleOAuth2SuccessHandler(
            AuthService authService,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.authService = authService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        try {
            if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
                throw new ServletException("Principal OIDC inválido");
            }

            authService.loginWithGoogle(oidcUser, response);
            response.sendRedirect(frontendUrl + "/dashboard");
        } catch (RuntimeException | ServletException exception) {
            response.sendRedirect(frontendUrl + "/login?googleError=true");
        } finally {
            if (request.getSession(false) != null) {
                request.getSession(false).invalidate();
            }
        }
    }
}
