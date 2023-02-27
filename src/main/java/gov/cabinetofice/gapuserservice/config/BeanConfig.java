package gov.cabinetofice.gapuserservice.config;

import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

@RequiredArgsConstructor
@Configuration
public class BeanConfig {

    private final ThirdPartyAuthProviderProperties thirdPartyAuthProviderProperties;

    @Bean
    public JwkProvider getJwkProvide() {
        return new UrlJwkProvider(thirdPartyAuthProviderProperties.getDomain());
    }

    @Bean
    public Mac getSha256Hmac() throws NoSuchAlgorithmException, InvalidKeyException {
        final String algorithm = HmacAlgorithms.HMAC_SHA_256.getName();
        final Mac mac = Mac.getInstance(algorithm);
        final SecretKeySpec secret_key = new SecretKeySpec(
                thirdPartyAuthProviderProperties.getSecretCookieKey().getBytes(StandardCharsets.UTF_8),
                algorithm
        );
        mac.init(secret_key);
        return mac;
    }

    @Bean
    public Calendar getCalendar() {
        return Calendar.getInstance();
    }
}
