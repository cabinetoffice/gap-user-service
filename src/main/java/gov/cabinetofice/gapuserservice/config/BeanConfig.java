package gov.cabinetofice.gapuserservice.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;

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
        final SecretKeySpec secretKey = new SecretKeySpec(
                thirdPartyAuthProviderProperties.getSecretCookieKey().getBytes(StandardCharsets.UTF_8),
                algorithm
        );
        mac.init(secretKey);
        return mac;
    }

    @Bean
    public Clock getClock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public PhoneNumberUtil getPhoneNumberUtil() {
        return PhoneNumberUtil.getInstance();
    }

    @Bean
    public AWSCognitoIdentityProvider getCognitoClientBuilder() {
        final BasicAWSCredentials awsCredentials = new BasicAWSCredentials(thirdPartyAuthProviderProperties.getAccessKey(), thirdPartyAuthProviderProperties.getSecretKey());
        return AWSCognitoIdentityProviderClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(thirdPartyAuthProviderProperties.getRegion())
                .build();
    }

    @Bean
    public static SecretsManagerClient getSecretsManagerClient() {
        return SecretsManagerClient.builder()
                .region(Region.EU_WEST_2)
                .build();
    }

    @Bean
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }
}
