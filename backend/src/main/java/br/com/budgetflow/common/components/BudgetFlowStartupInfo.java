package br.com.budgetflow.common.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

@Component
public class BudgetFlowStartupInfo {

    private static final Logger log = LoggerFactory.getLogger(BudgetFlowStartupInfo.class);

    @EventListener(ApplicationReadyEvent.class)
    public void printStartupInfo() {
        String port = System.getProperty("server.port", "8080");
        String localUrl = "http://localhost:" + port;
        String externalUrl = localUrl;
        try {
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            externalUrl = "http://" + hostAddress + ":" + port;
        } catch (Exception ignored) {}

        log.info("\n\nYour Application is running:\n  Local:    {}\n  External: {}\n", localUrl, externalUrl);
    }
}
