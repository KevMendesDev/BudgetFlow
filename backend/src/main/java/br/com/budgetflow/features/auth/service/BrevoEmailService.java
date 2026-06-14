package br.com.budgetflow.features.auth.service;

import br.com.budgetflow.features.users.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class BrevoEmailService {

    private final RestClient restClient;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;
    private final String frontendUrl;

    public BrevoEmailService(
            @Value("${app.email.brevo-api-key:}") String apiKey,
            @Value("${app.email.from:}") String fromEmail,
            @Value("${app.email.from-name:BudgetFlow}") String fromName,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.restClient = RestClient.builder().baseUrl("https://api.brevo.com/v3").build();
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.frontendUrl = frontendUrl;
    }

    public void sendVerification(User user, String token) {
        String url = frontendUrl + "/confirmar-email?token=" + token;
        send(
                user,
                "Confirme seu email no BudgetFlow",
                html("Confirme seu email", "Confirme seu cadastro para acessar o BudgetFlow.", url, "Confirmar email"));
    }

    public void sendPasswordReset(User user, String token) {
        String url = frontendUrl + "/redefinir-senha?token=" + token;
        send(
                user,
                "Redefina sua senha do BudgetFlow",
                html("Redefina sua senha", "Este link expira em 1 hora.", url, "Criar nova senha"));
    }

    private void send(User user, String subject, String htmlContent) {
        if (apiKey.isBlank() || fromEmail.isBlank()) {
            throw new IllegalStateException("Configuração de email ausente");
        }

        restClient.post()
                .uri("/smtp/email")
                .header("api-key", apiKey)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "sender", Map.of("name", fromName, "email", fromEmail),
                        "to", List.of(Map.of("name", user.getNome(), "email", user.getEmail())),
                        "subject", subject,
                        "htmlContent", htmlContent))
                .retrieve()
                .toBodilessEntity();
    }

    private String html(String title, String description, String url, String button) {
        return """
                <!doctype html>
                <html lang="pt-BR">
                  <body style="font-family:Arial,sans-serif;color:#162118">
                    <h1>%s</h1>
                    <p>%s</p>
                    <p><a href="%s" style="display:inline-block;padding:12px 18px;background:#12c978;color:#06130c;text-decoration:none;border-radius:8px">%s</a></p>
                    <p>Se você não solicitou isto, ignore este email.</p>
                  </body>
                </html>
                """.formatted(title, description, url, button);
    }
}
