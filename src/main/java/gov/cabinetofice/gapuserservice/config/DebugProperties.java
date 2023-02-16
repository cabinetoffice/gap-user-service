package gov.cabinetofice.gapuserservice.config;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "debug")
public class DebugProperties {

    private boolean ignoreJwt = false;

    private Integer userId = 1;

}