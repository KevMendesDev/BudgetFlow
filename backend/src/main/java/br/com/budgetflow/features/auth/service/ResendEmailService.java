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
        String title = "Confirme seu email";
        String description = "Confirme seu cadastro para acessar o BudgetFlow.";
        String button = "Confirmar email";
        send(
                user,
                "Confirme seu email no BudgetFlow",
                html(title, description, url, button),
                text(title, description, url));
    }

    public void sendPasswordReset(User user, String token) {
        String url = frontendUrl + "/redefinir-senha?token=" + token;
        String title = "Redefina sua senha";
        String description = "Este link expira em 1 hora.";
        String button = "Criar nova senha";
        send(
                user,
                "Redefina sua senha do BudgetFlow",
                html(title, description, url, button),
                text(title, description, url));
    }

    private void send(User user, String subject, String htmlContent, String textContent) {
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
                        "html", htmlContent,
                        "text", textContent))
                .retrieve()
                .toBodilessEntity();
    }

    private String html(String title, String description, String url, String button) {
        return """
                <!doctype html>
                <html lang="pt-BR">
                  <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <meta name="color-scheme" content="dark">
                    <meta name="supported-color-schemes" content="dark">
                  </head>
                  <body style="margin:0;padding:0;background-color:#0d121c">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" bgcolor="#0d121c" style="background-color:#0d121c">
                      <tr>
                        <td align="center" style="padding:24px 16px">
                          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" bgcolor="#0d121c" style="max-width:480px;background-color:#0d121c;font-family:Arial,Helvetica,sans-serif">
                            <tr>
                              <td style="padding-bottom:16px;color:#f4a327;font-size:14px;font-weight:bold">BudgetFlow</td>
                            </tr>
                            <tr>
                              <td style="font-size:22px;font-weight:bold;color:#f5f7fc;padding-bottom:12px">%s</td>
                            </tr>
                            <tr>
                              <td style="font-size:15px;line-height:1.5;color:#b3bccc;padding-bottom:24px">%s</td>
                            </tr>
                            <tr>
                              <td style="padding-bottom:24px">
                                <a href="%s" style="display:inline-block;padding:12px 20px;background-color:#12c978;color:#06130c;font-size:15px;font-weight:bold;text-decoration:none;border-radius:6px">%s</a>
                              </td>
                            </tr>
                            <tr>
                              <td style="font-size:13px;line-height:1.5;color:#727d91">Se você não solicitou esta ação, ignore este email.</td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>
                  </body>
                </html>
                """.formatted(title, description, url, button);
    }

    private String text(String title, String description, String url) {
        return """
                %s

                %s

                Acesse o link: %s

                Se você não solicitou esta ação, ignore este email.
                """.formatted(title, description, url);
    }
}
