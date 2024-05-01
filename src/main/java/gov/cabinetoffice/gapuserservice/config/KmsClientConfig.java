package gov.cabinetoffice.gapuserservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;

@Configuration
public class KmsClientConfig {

    private final KmsClient kmsClient;

    public KmsClientConfig() {
        this.kmsClient = KmsClient.builder().region(Region.EU_WEST_2).build();
    }

    @Bean
    public KmsClient getKmsClient() {
        return kmsClient;
    }
}
