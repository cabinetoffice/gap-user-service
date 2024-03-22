package gov.cabinetofice.gapuserservice.config;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Configuration("serviceToServiceConfigurationProperties")
@ConfigurationProperties(prefix = "service-to-service")

public class ServiceToServiceConfigProperties {
    private String secret;
    private String privateKey;
}
