package br.com.budgetflow.common.components;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

@Component
public class BudgetFlowStartupInfo {

    @EventListener(ApplicationReadyEvent.class)
    public void printStartupInfo() {
        String port = System.getProperty("server.port", "8080");
        String localUrl = "http://localhost:" + port;
        String externalUrl = localUrl;
        try {
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            externalUrl = "http://" + hostAddress + ":" + port;
        } catch (Exception ignored) {}

        System.out.println("\n\n" +
            "Your Application is running:\n" +
            "  Local:    " + localUrl + "\n" +
            "  External: " + externalUrl + "\n"
        );
    }
}