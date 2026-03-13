package br.com.budgetflow.common.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserPingController {

    @GetMapping("/ping")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public String ping() {
        return "pong (user)";
    }
}
