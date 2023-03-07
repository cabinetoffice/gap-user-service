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

    private String secretKey;

    private String accessKey;

    private String region;

    private String userPoolId;

    private String userPassword;

    private String domain;

    private String appClientId;

    private String secretCookieKey;

    private String tokenCookie;

    private String logoutUrl;

    private String loginUrl;
}
