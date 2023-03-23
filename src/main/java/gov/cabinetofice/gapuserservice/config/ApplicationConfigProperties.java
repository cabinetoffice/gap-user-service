package gov.cabinetofice.gapuserservice.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Configuration("applicationConfigProperties")
@ConfigurationProperties
public class ApplicationConfigProperties {

    @Value("${spring.profiles.active:PROD}")
    private String profile;
    private String defaultRedirectUrl;
}
