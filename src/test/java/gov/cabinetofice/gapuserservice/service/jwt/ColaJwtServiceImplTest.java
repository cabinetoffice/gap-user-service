package gov.cabinetofice.gapuserservice.service.jwt;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import gov.cabinetofice.gapuserservice.config.ThirdPartyAuthProviderProperties;
import gov.cabinetofice.gapuserservice.exceptions.JwkNotValidTokenException;
import gov.cabinetofice.gapuserservice.service.jwt.impl.ColaJwtServiceImpl;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static com.auth0.jwt.JWT.decode;
import static com.auth0.jwt.algorithms.Algorithm.RSA256;
import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ColaJwtServiceImplTest {

    @Mock
    private JwkProvider jwkProvider;

    @Mock
    private Mac mac;

    private ColaJwtServiceImpl serviceUnderTest;

    private final String VALID_COLA_SIGNATURE = "colaSignature";
    private final String INVALID_COLA_SIGNATURE = "invalidColaSignature";

    @BeforeEach
    void setup() {
        final ThirdPartyAuthProviderProperties thirdPartyAuthProviderProperties = ThirdPartyAuthProviderProperties.builder()
                .accessKey("an-access-key")
                .secretKey("a-secret-key")
                .userPoolId("a-user-pool-id")
                .region("eu-west-2")
                .userPassword("a-user-password")
                .domain("domain")
                .appClientId("appClientId")
                .secretCookieKey("secretCookieKey")
                .build();

        serviceUnderTest = new ColaJwtServiceImpl(thirdPartyAuthProviderProperties, jwkProvider, mac);
    }

    private String generateJwt(final JwtBuilder jwtBuilder) {
        return jwtBuilder.compact() + ".Y29sYVNpZ25hdHVyZQ";
    }

    @Test
    void verifyToken_expiredToken() throws JwkException {
        final Date expiresAt = Date.from(now().minus(1, ChronoUnit.DAYS));
        final String jwt = generateJwt(Jwts.builder()
                .setExpiration(expiresAt));

        when(mac.doFinal(any())).thenReturn(VALID_COLA_SIGNATURE.getBytes());
        final Jwk jwk = mock(Jwk.class);
        when(jwkProvider.get(anyString())).thenReturn(jwk);
        when(jwk.getPublicKey()).thenReturn(null);

        try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
            try (MockedStatic<Algorithm> staticAlgorithm = Mockito.mockStatic(Algorithm.class)) {
                final Algorithm algorithm = mock(Algorithm.class);
                staticAlgorithm.when(() -> RSA256(any(), eq(null))).thenReturn(algorithm);

                final DecodedJWT testDecodedJwt = Mockito.spy(TestDecodedJwt.builder().keyId("signingKey").expiresAt(expiresAt).build());

                staticJwt.when(() -> decode(anyString())).thenReturn(testDecodedJwt);

                final boolean methodResponse = serviceUnderTest.isTokenValid(jwt);

                verify(testDecodedJwt, times(1)).getExpiresAt();
                assertThat(methodResponse).isFalse();
            }
        }
    }

    private static Stream<Arguments> provideDecodedJwtArgs() {
        final Arguments wrongIssuer = Arguments.of(TestDecodedJwt.builder()
                .keyId("signingKey")
                .expiresAt(Date.from(now().plus(1, ChronoUnit.DAYS)))
                .issuer("wrong-issuer")
                .audience(List.of("appClientId"))
                .build());
        final Arguments wrongAudience = Arguments.of(TestDecodedJwt.builder()
                .keyId("signingKey")
                .expiresAt(Date.from(now().plus(1, ChronoUnit.DAYS)))
                .issuer("domain")
                .audience(List.of("wrong-audience"))
                .build());
        return Stream.of(wrongIssuer, wrongAudience);
    }

    @ParameterizedTest
    @MethodSource("provideDecodedJwtArgs")
    void verifyToken_ThrowErrorWhenNotExpectedIssuerOrAudience(final DecodedJWT decodedJWT) throws JwkException {
        final Date expiresAt = Date.from(now().plus(1, ChronoUnit.DAYS));
        final String jwt = generateJwt(Jwts.builder()
                .setExpiration(expiresAt));

        when(mac.doFinal(any())).thenReturn(VALID_COLA_SIGNATURE.getBytes());
        final Jwk jwk = mock(Jwk.class);
        when(jwkProvider.get(anyString())).thenReturn(jwk);
        when(jwk.getPublicKey()).thenReturn(null);

        try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
            try (MockedStatic<Algorithm> staticAlgorithm = Mockito.mockStatic(Algorithm.class)) {
                final Algorithm algorithm = mock(Algorithm.class);
                staticAlgorithm.when(() -> RSA256(any(), eq(null))).thenReturn(algorithm);

                final DecodedJWT testDecodedJwt = Mockito.spy(decodedJWT);

                staticJwt.when(() -> decode(anyString())).thenReturn(testDecodedJwt);

                final JwkNotValidTokenException response = assertThrows(JwkNotValidTokenException.class,
                        () -> serviceUnderTest.isTokenValid(jwt));
                assertThat(response.getMessage()).isEqualTo("Third party token is not valid");
            }
        }
    }

    @Test
    void verifyToken_InvalidColaSignature() {
        final Logger logger = (Logger) LoggerFactory.getLogger(ColaJwtServiceImpl.class);
        final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        final List<ILoggingEvent> logsList = listAppender.list;
        final JwtBuilder jwtBuilder = Jwts.builder().setExpiration(Date.from(now()));
        final String jwt = generateJwt(jwtBuilder);

        when(mac.doFinal(any())).thenReturn(INVALID_COLA_SIGNATURE.getBytes());

        final boolean methodResponse = serviceUnderTest.isTokenValid(jwt);

        verify(mac, times(1)).doFinal(jwtBuilder.compact().getBytes(StandardCharsets.UTF_8));
        assertThat(methodResponse).isFalse();
        assertEquals("COLAs JWT signature is invalid", logsList.get(0)
                .getMessage());
        assertEquals(Level.ERROR, logsList.get(0)
                .getLevel());
    }

    @Test
    void verifyToken_ThrowsJwtDecodeException_ifTokenCannotBeDecoded() {

        final Date expiresAt = Date.from(now().minus(1, ChronoUnit.DAYS));
        final String jwt = generateJwt(Jwts.builder()
                .setExpiration(expiresAt));

        when(mac.doFinal(any())).thenReturn(VALID_COLA_SIGNATURE.getBytes());

        try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
            staticJwt.when(() -> decode(any())).thenThrow(JWTDecodeException.class);

            assertThrows(JWTDecodeException.class,
                    () -> serviceUnderTest.isTokenValid(jwt));
        }
    }

    @Test
    void verifyToken_ReturnsFalse_IfJwtSignatureIsInvalid() throws JwkException {
        final Date expiresAt = Date.from(now().minus(1, ChronoUnit.DAYS));
        final String jwt = generateJwt(Jwts.builder()
                .setExpiration(expiresAt));

        when(mac.doFinal(any())).thenReturn(VALID_COLA_SIGNATURE.getBytes());
        final Jwk jwk = mock(Jwk.class);

        when(jwkProvider.get(anyString())).thenReturn(jwk);
        when(jwk.getPublicKey()).thenReturn(null);

        try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
            try (MockedStatic<Algorithm> staticAlgorithm = Mockito.mockStatic(Algorithm.class)) {
                final Algorithm algorithm = mock(Algorithm.class);
                staticAlgorithm.when(() -> RSA256(any(), eq(null))).thenReturn(algorithm);

                final DecodedJWT testDecodedJwt = Mockito.spy(TestDecodedJwt.builder().keyId("signingKey").expiresAt(expiresAt).build());
                staticJwt.when(() -> decode(anyString())).thenReturn(testDecodedJwt);

                doThrow(SignatureVerificationException.class).when(algorithm).verify(testDecodedJwt);

                final boolean methodResponse = serviceUnderTest.isTokenValid(jwt);

                verify(algorithm).verify(testDecodedJwt);
                assertThat(methodResponse).isFalse();
            }
        }
    }

    @Test
    void verifyToken_ReturnsTrue_IfTokenIsValid() throws JwkException {
        final Date expiresAt = Date.from(now().plus(1, ChronoUnit.DAYS));
        final String jwt = generateJwt(Jwts.builder()
                .setIssuer("domain")
                .setExpiration(expiresAt));

        when(mac.doFinal(any())).thenReturn(VALID_COLA_SIGNATURE.getBytes());
        final Jwk jwk = mock(Jwk.class);
        when(jwkProvider.get(anyString())).thenReturn(jwk);
        when(jwk.getPublicKey()).thenReturn(null);

        try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
            try (MockedStatic<Algorithm> staticAlgorithm = Mockito.mockStatic(Algorithm.class)) {
                final Algorithm algorithm = mock(Algorithm.class);
                staticAlgorithm.when(() -> RSA256(any(), eq(null))).thenReturn(algorithm);

                final DecodedJWT testDecodedJwt = Mockito.spy(TestDecodedJwt.builder()
                        .keyId("signingKey")
                        .expiresAt(expiresAt)
                        .issuer("domain")
                        .audience(List.of("appClientId"))
                        .build());

                staticJwt.when(() -> decode(anyString())).thenReturn(testDecodedJwt);

                final boolean methodResponse = serviceUnderTest.isTokenValid(jwt);

                assertThat(methodResponse).isTrue();
            }
        }
    }
}
