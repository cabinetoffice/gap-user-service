package gov.cabinetoffice.gapuserservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebConfig {
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}