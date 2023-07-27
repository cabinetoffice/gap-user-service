package gov.cabinetofice.gapuserservice.service.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Verification;
import com.nimbusds.jose.JOSEException;
import gov.cabinetofice.gapuserservice.config.JwtProperties;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.repository.JwtBlacklistRepository;
import gov.cabinetofice.gapuserservice.repository.UserRepository;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.auth0.jwt.JWT.decode;
import static com.auth0.jwt.JWT.require;
import static com.auth0.jwt.algorithms.Algorithm.RSA256;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class CustomJwtServiceImplTest {

    private CustomJwtServiceImpl serviceUnderTest;

    @Mock
    private JwtBlacklistRepository jwtBlacklistRepository;

    @Mock
    private UserRepository userRepository;

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

        serviceUnderTest = spy(new CustomJwtServiceImpl(jwtProperties, jwtBlacklistRepository, userRepository, clock));
    }

    @Nested
    class IsTokenValid {
        @Mock
        private final JWTVerifier mockedJwtVerifier = mock(JWTVerifier.class);
        final Algorithm mockAlgorithm = mock(Algorithm.class);
        private final Verification verification = JWT.require(mockAlgorithm);

        final String jwt = "eyJraWQiOiIxODNjZTQ5YS0xYjExLTRiYjgtOWExMi1iYTViNzVmNGVmZjQiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOmliZDJyejJDZ3lpZG5kWHlxMnp5ZmNuUXd5WUk1N2gzNHZNbFNyODdDRGYiLCJhdWQiOiJGR1AiLCJyb2xlcyI6IltBUFBMSUNBTlQsIEZJTkQsIEFETUlOLCBTVVBFUl9BRE1JTl0iLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODIiLCJkZXBhcnRtZW50IjoiQ2FiaW5ldCBPZmZpY2UiLCJleHAiOjE2OTA0NTI5MzQsImlhdCI6MTY5MDQ0OTMzNCwiZW1haWwiOiJ0ZXN0LnVzZXJAZ292LnVrIiwianRpIjoiMTlkMjBmMzItMWQyMi00NDc4LWI5MDYtMDM3OTM4OTg1MTgzIn0.fBFYmFn7cUMluJBRg5poJFUql0xyShja98xPdIrBmkAO8HZmMnCw8Nh--4jDvPdqquHShfWUZrdn2ZMTCBA_ClYKrYtnZ2NAD1F6RnJm0scQ5LiPbyEfCh52qTWweXApPsmDfR4P_XJKTrpGjLMVvaoDUMAPy6IVu7ZYYn8sTB6RsQI16B_tbUkpnhJJcayPAUPa5FcIKgmMbOH_k1KpgmNizGLsd--JV4b7WXQ4BZM_6aRUcELXwyNdMCjiyWOGtU0gB3e0fb1zvTZ0ED7yNAWk5eUaRVgOD30NRsms_gf3EALDn7XTjXo6NPwXH_K4tRZSces5HllVPM04Z9lHpg";


        @Test
        void ReturnsTrue_IfValid() {
            final Algorithm mockAlgorithm = mock(Algorithm.class);
            final Verification spiedVerification = spy(verification);

            try (MockedStatic<Algorithm> staticAlgorithm = Mockito.mockStatic(Algorithm.class)) {
                staticAlgorithm.when(() -> RSA256(any(), any())).thenReturn(mockAlgorithm);
                try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
                    staticJwt.when(() -> require(any())).thenReturn(spiedVerification);
                    staticJwt.when(() -> decode(any())).thenCallRealMethod();
                    when(spiedVerification.build()).thenReturn(mockedJwtVerifier);
                    when(userRepository.findBySub(any())).thenReturn(Optional.of(User.builder().acceptedPrivacyPolicy(true).build()));


                    final boolean response = serviceUnderTest.isTokenValid(jwt);
                    assertThat(response).isTrue();
                    verify(mockedJwtVerifier, times(1)).verify(jwt);
                }
            }
        }

        @Test
        void ReturnsFalse_IfInvalid() {
            final Algorithm mockAlgorithm = mock(Algorithm.class);
            final Verification spiedVerification = spy(verification);

            try (MockedStatic<Algorithm> staticAlgorithm = Mockito.mockStatic(Algorithm.class)) {
                staticAlgorithm.when(() -> RSA256(any(), any())).thenReturn(mockAlgorithm);
                try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
                    staticJwt.when(() -> require(any())).thenReturn(spiedVerification);
                    staticJwt.when(() -> decode(any())).thenCallRealMethod();
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
            final Algorithm mockAlgorithm = mock(Algorithm.class);
            final Verification spiedVerification = spy(verification);

            try (MockedStatic<Algorithm> staticAlgorithm = Mockito.mockStatic(Algorithm.class)) {
                staticAlgorithm.when(() -> RSA256(any(), any())).thenReturn(mockAlgorithm);
                try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
                    staticJwt.when(() -> require(any())).thenReturn(spiedVerification);
                    staticJwt.when(() -> decode(any())).thenCallRealMethod();
                    when(spiedVerification.build()).thenReturn(mockedJwtVerifier);
                    when(jwtBlacklistRepository.existsByJwtIs(jwt)).thenReturn(true);
                    when(userRepository.findBySub(any())).thenReturn(Optional.of(User.builder().acceptedPrivacyPolicy(true).build()));

                    final boolean response = serviceUnderTest.isTokenValid(jwt);

                    assertThat(response).isFalse();
                    verify(mockedJwtVerifier, times(1)).verify(jwt);
                    verify(jwtBlacklistRepository, atLeastOnce()).existsByJwtIs(jwt);
                }
            }
        }

        @Test
        void SignsWithCorrectAlgorithm() {
            final Algorithm mockAlgorithm = mock(Algorithm.class);
            final Verification spiedVerification = spy(verification);

            try (MockedStatic<Algorithm> staticAlgorithm = Mockito.mockStatic(Algorithm.class)) {
                try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
                    staticJwt.when(() -> require(any())).thenReturn(spiedVerification);
                    staticJwt.when(() -> decode(any())).thenCallRealMethod();
                    when(spiedVerification.build()).thenReturn(mockedJwtVerifier);
                    staticAlgorithm.when(() -> RSA256(any(), any())).thenReturn(mockAlgorithm);
                    when(userRepository.findBySub(any())).thenReturn(Optional.of(User.builder().acceptedPrivacyPolicy(true).build()));

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
