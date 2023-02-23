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
@Configuration("applicationConfigProperties")
@ConfigurationProperties
public class ApplicationConfigProperties {
    private String defaultRedirectUrl;
}
