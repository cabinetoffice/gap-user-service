package gov.cabinetoffice.gapuserservice.config;

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
@Configuration("findAGrantConfigProperties")
@ConfigurationProperties(prefix = "find-a-grant")
public class FindAGrantConfigProperties {
    private String url;
}
