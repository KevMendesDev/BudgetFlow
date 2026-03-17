package br.com.budgetflow.security;

import br.com.budgetflow.common.exceptions.UnauthorizedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("Usuário não autenticado");
        }

        String userId = auth.getName();
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new UnauthorizedException("Token inválido: subject não é um id numérico");
        }
    }
}