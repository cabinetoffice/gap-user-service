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
@Configuration("thirdPartyAuthProviderProperties")
@ConfigurationProperties(prefix = "authentication-provider")
public class ThirdPartyAuthProviderProperties {
    private String url;
}
