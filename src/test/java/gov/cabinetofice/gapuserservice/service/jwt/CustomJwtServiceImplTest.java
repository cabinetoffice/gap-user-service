package gov.cabinetofice.gapuserservice.service.jwt;

import com.auth0.jwt.JWT;
import static com.auth0.jwt.JWT.require;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import static com.auth0.jwt.algorithms.Algorithm.HMAC256;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import com.nimbusds.jose.JOSEException;
import gov.cabinetofice.gapuserservice.config.JwtProperties;
import gov.cabinetofice.gapuserservice.repository.JwtBlacklistRepository;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;

import static com.auth0.jwt.algorithms.Algorithm.RSA256;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@ExtendWith(MockitoExtension.class)
public class CustomJwtServiceImplTest {

    private CustomJwtServiceImpl serviceUnderTest;

    @Mock
    private JwtBlacklistRepository jwtBlacklistRepository;

    private final String CHRISTMAS_2022_MIDDAY = "2022-12-25T12:00:00.00z";
    private final Clock clock = Clock.fixed(Instant.parse(CHRISTMAS_2022_MIDDAY), ZoneId.of("UTC"));

    @BeforeEach
    void setup() throws JOSEException {
        final JwtProperties jwtProperties = JwtProperties.builder()
                .signingKey("test-signing-key")
                .issuer("test-issuer")
                .audience("test-audience")
                .expiresAfter(60)
                .build();

        serviceUnderTest = spy(new CustomJwtServiceImpl(jwtProperties, jwtBlacklistRepository, clock));
    }

    @Nested
    class IsTokenValid {
        @Mock
        private final JWTVerifier mockedJwtVerifier = mock(JWTVerifier.class);
        final Algorithm mockAlgorithm = mock(Algorithm.class);
        private final Verification verification = JWT.require(mockAlgorithm);

        @Test
        void ReturnsTrue_IfValid() {
            final String jwt = "a-valid-jwt";
            final Algorithm mockAlgorithm = mock(Algorithm.class);
            final Verification spiedVerification = spy(verification);

            try (MockedStatic<Algorithm> staticAlgorithm = Mockito.mockStatic(Algorithm.class)) {
                staticAlgorithm.when(() -> RSA256(any(), any())).thenReturn(mockAlgorithm);
                try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
                    staticJwt.when(() -> require(any())).thenReturn(spiedVerification);
                    when(spiedVerification.build()).thenReturn(mockedJwtVerifier);

                    final boolean response = serviceUnderTest.isTokenValid(jwt);
                    assertThat(response).isTrue();
                    verify(mockedJwtVerifier, times(1)).verify(jwt);
                }
            }
        }

        @Test
        void ReturnsFalse_IfInvalid() {
            final String jwt = "an-invalid-jwt";
            final Algorithm mockAlgorithm = mock(Algorithm.class);
            final Verification spiedVerification = spy(verification);

            try (MockedStatic<Algorithm> staticAlgorithm = Mockito.mockStatic(Algorithm.class)) {
                staticAlgorithm.when(() -> RSA256(any(), any())).thenReturn(mockAlgorithm);
                try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
                    staticJwt.when(() -> require(any())).thenReturn(spiedVerification);
                    when(spiedVerification.build()).thenReturn(mockedJwtVerifier);
                    when(mockedJwtVerifier.verify(jwt)).thenThrow(new JWTVerificationException("An error"));

                    final boolean response = serviceUnderTest.isTokenValid(jwt);
                    assertThat(response).isFalse();
                    verify(mockedJwtVerifier, times(1)).verify(jwt);
                }
            }
        }

        @Test
        void ReturnsFalse_IfBlacklisted() {
            final String jwt = "a-valid-jwt";
            final Algorithm mockAlgorithm = mock(Algorithm.class);
            final Verification spiedVerification = spy(verification);

            try (MockedStatic<Algorithm> staticAlgorithm = Mockito.mockStatic(Algorithm.class)) {
                staticAlgorithm.when(() -> RSA256(any(), any())).thenReturn(mockAlgorithm);
                try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
                    staticJwt.when(() -> require(any())).thenReturn(spiedVerification);
                    when(spiedVerification.build()).thenReturn(mockedJwtVerifier);
                    when(jwtBlacklistRepository.existsByJwtIs(jwt)).thenReturn(true);

                    final boolean response = serviceUnderTest.isTokenValid(jwt);

                    assertThat(response).isFalse();
                    verify(mockedJwtVerifier, times(1)).verify(jwt);
                    verify(jwtBlacklistRepository, atLeastOnce()).existsByJwtIs(jwt);
                }
            }
        }

        @Test
        void SignsWithCorrectAlgorithm() {
            final String jwt = "a-jwt";
            final Algorithm mockAlgorithm = mock(Algorithm.class);
            final Verification spiedVerification = spy(verification);

            try (MockedStatic<Algorithm> staticAlgorithm = Mockito.mockStatic(Algorithm.class)) {
                try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
                    staticJwt.when(() -> require(any())).thenReturn(spiedVerification);
                    when(spiedVerification.build()).thenReturn(mockedJwtVerifier);
                    staticAlgorithm.when(() -> RSA256(any(), any())).thenReturn(mockAlgorithm);

                    serviceUnderTest.isTokenValid(jwt);

                    staticAlgorithm.verify(() -> RSA256(any(), any()), times(1));
                    staticJwt.verify(() -> JWT.require(mockAlgorithm), times(1));
                    verify(mockedJwtVerifier, times(1)).verify(jwt);
                }
            }
        }
    }

    @Nested
    class GenerateToken {

        private final DecodedJWT thirdPartyToken = TestDecodedJwt.builder().build();


        @Test
        void WithCorrectIssuer() {
            final JWTCreator.Builder mockedJwtBuilder = spy(JWTCreator.Builder.class);
            final Map<String, String> claims = new HashMap<>();
            claims.put("test-claim-key", "test-claim-value");

            try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
                staticJwt.when(JWT::create).thenReturn(mockedJwtBuilder);

                serviceUnderTest.generateToken(claims);

                verify(mockedJwtBuilder, times(1)).withIssuer("test-issuer");
            }
        }

        @Test
        void WithCorrectAudience() {
            final JWTCreator.Builder mockedJwtBuilder = spy(JWTCreator.Builder.class);
            final Map<String, String> claims = new HashMap<>();
            claims.put("test-claim-key", "test-claim-value");
            try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
                staticJwt.when(JWT::create).thenReturn(mockedJwtBuilder);

                serviceUnderTest.generateToken(claims);

                verify(mockedJwtBuilder, times(1)).withAudience("test-audience");
            }
        }

        @Test
        void WithCorrectExpiration() {
            final JWTCreator.Builder mockedJwtBuilder = spy(JWTCreator.Builder.class);
            final long now = ZonedDateTime.now(clock).toInstant().toEpochMilli();
            final Map<String, String> claims = new HashMap<>();
            claims.put("test-claim-key", "test-claim-value");

            try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
                staticJwt.when(JWT::create).thenReturn(mockedJwtBuilder);

                serviceUnderTest.generateToken(claims);

                verify(mockedJwtBuilder, times(1)).withExpiresAt(new Date(now + 60 * 1000 * 60));
            }
        }

        @Test
        void WithCorrectSigningKey() {
            final JWTCreator.Builder mockedJwtBuilder = spy(JWTCreator.Builder.class);
            final Algorithm mockAlgorithm = mock(Algorithm.class);
            final Map<String, String> claims = new HashMap<>();
            claims.put("test-claim-key", "test-claim-value");

            try (MockedStatic<Algorithm> staticAlgorithm = Mockito.mockStatic(Algorithm.class)) {
                try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
                    staticJwt.when(JWT::create).thenReturn(mockedJwtBuilder);
                    staticAlgorithm.when(() -> RSA256(any(), any())).thenReturn(mockAlgorithm);
                    doReturn("a-custom-jwt").when(mockedJwtBuilder).sign(mockAlgorithm);

                    serviceUnderTest.generateToken(claims);

                    staticAlgorithm.verify(() -> RSA256(any(), any()), times(1));
                    verify(mockedJwtBuilder, times(1)).sign(mockAlgorithm);
                }
            }
        }

        @Test
        void ReturnsACustomJwt() {
            final JWTCreator.Builder mockedJwtBuilder = spy(JWTCreator.Builder.class);
            final Map<String, String> claims = new HashMap<>();
            claims.put("test-claim-key", "test-claim-value");

            try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
                staticJwt.when(JWT::create).thenReturn(mockedJwtBuilder);
                doReturn("a-custom-jwt").when(mockedJwtBuilder).sign(any());

                final String response = serviceUnderTest.generateToken(claims);

                assertThat(response).isEqualTo("a-custom-jwt");
            }
        }
    }
}
