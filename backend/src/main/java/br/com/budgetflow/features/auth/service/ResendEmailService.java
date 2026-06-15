package br.com.budgetflow.features.auth.service;

import br.com.budgetflow.features.users.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class ResendEmailService {

    private final RestClient restClient;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;
    private final String frontendUrl;

    public ResendEmailService(
            @Value("${app.email.resend-api-key:}") String apiKey,
            @Value("${app.email.from:}") String fromEmail,
            @Value("${app.email.from-name:BudgetFlow}") String fromName,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("User-Agent", "BudgetFlow/3.0")
                .build();
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
                .uri("/emails")
                .header("Authorization", "Bearer " + apiKey)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "from", "%s <%s>".formatted(fromName, fromEmail),
                        "to", List.of(user.getEmail()),
                        "subject", subject,
                        "html", htmlContent))
                .retrieve()
                .toBodilessEntity();
    }

    private String html(String title, String description, String url, String button) {
        return """
                <!doctype html>
                <html lang="pt-BR">
                  <body style="margin:0;padding:0;background:#070a11;font-family:Arial,sans-serif;color:#f5f7fc">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background:#070a11">
                      <tr>
                        <td align="center" style="padding:40px 16px">
                          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width:560px;background:#0d121c;border:1px solid #293140;border-radius:16px">
                            <tr>
                              <td style="padding:32px">
                                <div style="display:inline-block;padding:6px 12px;border:1px solid #293140;border-radius:999px;color:#f4a327;font-size:12px;font-weight:700;letter-spacing:1px;text-transform:uppercase">
                                  BudgetFlow
                                </div>
                                <h1 style="margin:24px 0 12px;font-size:30px;line-height:1.2;color:#f5f7fc">%s</h1>
                                <p style="margin:0;color:#9ea8bb;font-size:16px;line-height:1.6">%s</p>
                                <table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin-top:28px">
                                  <tr>
                                    <td style="border-radius:12px;background:#12c978">
                                      <a href="%s" style="display:inline-block;padding:14px 22px;color:#06130c;font-size:15px;font-weight:700;text-decoration:none">%s</a>
                                    </td>
                                  </tr>
                                </table>
                                <div style="height:1px;background:#293140;margin:32px 0 20px"></div>
                                <p style="margin:0;color:#727d91;font-size:13px;line-height:1.5">
                                  Se você não solicitou esta ação, ignore este email com segurança.
                                </p>
                              </td>
                            </tr>
                          </table>
                          <p style="margin:18px 0 0;color:#596477;font-size:12px">
                            BudgetFlow · Controle teu dinheiro sem caos
                          </p>
                        </td>
                      </tr>
                    </table>
                  </body>
                </html>
                """.formatted(title, description, url, button);
    }
}
