package gov.cabinetofice.gapuserservice.config;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static javax.crypto.Mac.getInstance;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BeanConfigTest {

    private BeanConfig configUnderTest;

    @Test
    void getSha256Hmac_throwsError_EmptySecretCookieKey() {
        final ThirdPartyAuthProviderProperties thirdPartyAuthProviderProperties = ThirdPartyAuthProviderProperties.builder()
                .secretCookieKey("")
                .build();
        configUnderTest = new BeanConfig(thirdPartyAuthProviderProperties);

        final IllegalArgumentException response = assertThrows(IllegalArgumentException.class, () -> configUnderTest.getSha256Hmac());
        assertThat(response.getMessage()).isEqualTo("Empty key");
    }

    @Test
    void getSha256Hmac_throwsError_NullSecretCookieKey() {
        final ThirdPartyAuthProviderProperties thirdPartyAuthProviderProperties = ThirdPartyAuthProviderProperties.builder().build();
        configUnderTest = new BeanConfig(thirdPartyAuthProviderProperties);

        assertThrows(NullPointerException.class, () -> configUnderTest.getSha256Hmac());
    }

    @Test
    void getSha256Hmac_throwsError_InvalidAlgorithm() {
        final ThirdPartyAuthProviderProperties thirdPartyAuthProviderProperties = ThirdPartyAuthProviderProperties.builder()
                .domain("domain")
                .secretCookieKey("secretCookieKey")
                .build();
        configUnderTest = new BeanConfig(thirdPartyAuthProviderProperties);

        try (MockedStatic<Mac> mac = Mockito.mockStatic(Mac.class)) {
            mac.when(() -> getInstance(anyString())).thenThrow(new NoSuchAlgorithmException());

            assertThrows(NoSuchAlgorithmException.class, () -> configUnderTest.getSha256Hmac());
        }
    }

    @Test
    void getSha256Hmac_returnsSha256MacWithSecret() throws NoSuchAlgorithmException, InvalidKeyException {
        final String secretCookieKey = "validSecretCookieKey";
        final String algorithm = HmacAlgorithms.HMAC_SHA_256.getName();
        final ThirdPartyAuthProviderProperties thirdPartyAuthProviderProperties = ThirdPartyAuthProviderProperties.builder()
                .secretCookieKey(secretCookieKey)
                .build();

        configUnderTest = new BeanConfig(thirdPartyAuthProviderProperties);

        try (MockedStatic<Mac> mac = Mockito.mockStatic(Mac.class)) {
            final Mac macMock = mock(Mac.class);
            mac.when(() -> getInstance(anyString())).thenReturn(macMock);

            configUnderTest.getSha256Hmac();

            final SecretKeySpec secret_key = new SecretKeySpec(
                    secretCookieKey.getBytes(StandardCharsets.UTF_8),
                    algorithm
            );
            mac.verify(() -> getInstance(algorithm), times(1));
            verify(macMock, times(1)).init(secret_key);
        }
    }
}
