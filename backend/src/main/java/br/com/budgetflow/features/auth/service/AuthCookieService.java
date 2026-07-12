package br.com.budgetflow.features.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthCookieService {

    private final boolean secure;
    private final String sameSite;
    private final String domain;
    private final String contextPath;

    public AuthCookieService(
            @Value("${app.security.cookies.secure}") boolean secure,
            @Value("${app.security.cookies.same-site}") String sameSite,
            @Value("${app.security.cookies.domain}") String domain,
            @Value("${server.servlet.context-path:/}") String contextPath) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.domain = domain;
        this.contextPath = contextPath.isBlank() ? "/" : contextPath;
    }

    public void setAccessTokenCookie(HttpServletResponse response, String token, int maxAgeSeconds) {
        addCookie(response, "access_token", token, maxAgeSeconds);
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String token, int maxAgeSeconds) {
        addCookie(response, "refresh_token", token, maxAgeSeconds);
    }

    public void clearCookies(HttpServletResponse response) {
        addCookie(response, "access_token", "", 0);
        addCookie(response, "refresh_token", "", 0);
        clearHostOnlyCookie(response, "JSESSIONID", true, contextPath);
        clearHostOnlyCookie(response, "XSRF-TOKEN", false, "/");
    }

    private void clearHostOnlyCookie(HttpServletResponse response, String name, boolean httpOnly, String path) {
        StringBuilder header = new StringBuilder(String.format(
                "%s=; Path=%s; Max-Age=0",
                name, path));
        if (httpOnly) {
            header.append("; HttpOnly");
        }
        if (secure) {
            header.append("; Secure");
        }
        header.append("; SameSite=").append(sameSite);
        response.addHeader("Set-Cookie", header.toString());
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        StringBuilder header = new StringBuilder(String.format(
                "%s=%s; Path=/; Max-Age=%d; HttpOnly",
                name, value, maxAge));
        if (secure) {
            header.append("; Secure");
        }
        header.append("; SameSite=").append(sameSite);
        if (domain != null && !domain.isBlank()) {
            header.append("; Domain=").append(domain);
        }
        response.addHeader("Set-Cookie", header.toString());
    }
}
