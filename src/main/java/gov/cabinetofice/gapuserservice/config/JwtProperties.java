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
@Configuration("jwtProperties")
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * How long the JWT is valid before it expires, in minutes
     */
    private Integer expiresAfter;

    private String signingKey;

    private String issuer;

    private String audience;

    private String cookieName;


}
