package gov.cabinetofice.gapuserservice.service.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import gov.cabinetofice.gapuserservice.config.JwtProperties;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Calendar;
import java.util.Date;

import static com.auth0.jwt.JWT.require;
import static com.auth0.jwt.algorithms.Algorithm.HMAC256;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomJwtServiceImplTest {

    private CustomJwtServiceImpl serviceUnderTest;

    @Mock
    private Calendar calendar;

    @BeforeEach
    void setup() {
        final JwtProperties jwtProperties = JwtProperties.builder()
                .signingKey("test-signing-key")
                .issuer("test-issuer")
                .audience("test-audience")
                .expiresAfter(60)
                .build();

        serviceUnderTest = new CustomJwtServiceImpl(jwtProperties, calendar);
    }

    @Nested
    class IsTokenValid {
        @Mock
        private final JWTVerifier mockedJwtVerifier = mock(JWTVerifier.class);
        private final Verification verification = JWT.require(HMAC256("test-secret"));

        @Test
        void ReturnsTrue_IfValid() {
            final String jwt = "a-valid-jwt";
            final Verification spiedVerification = spy(verification);

            try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
                staticJwt.when(() -> require(any())).thenReturn(spiedVerification);
                when(spiedVerification.build()).thenReturn(mockedJwtVerifier);

                final boolean response = serviceUnderTest.isTokenValid(jwt);

                assertThat(response).isTrue();
                verify(mockedJwtVerifier, times(1)).verify(jwt);
            }
        }

        @Test
        void ReturnsFalse_IfInvalid() {
            final String jwt = "an-invalid-jwt";
            final Verification spiedVerification = spy(verification);

            try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
                staticJwt.when(() -> require(any())).thenReturn(spiedVerification);
                when(spiedVerification.build()).thenReturn(mockedJwtVerifier);
                when(mockedJwtVerifier.verify(jwt)).thenThrow(new JWTVerificationException("An error"));

                final boolean response = serviceUnderTest.isTokenValid(jwt);

                assertThat(response).isFalse();
                verify(mockedJwtVerifier, times(1)).verify(jwt);
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
                    staticAlgorithm.when(() -> HMAC256("test-signing-key")).thenReturn(mockAlgorithm);

                    serviceUnderTest.isTokenValid(jwt);

                    staticAlgorithm.verify(() -> HMAC256("test-signing-key"), times(1));
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

            try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
                staticJwt.when(() -> JWT.create()).thenReturn(mockedJwtBuilder);

                serviceUnderTest.generateToken();

                verify(mockedJwtBuilder, times(1)).withIssuer("test-issuer");
            }
        }

        @Test
        void WithCorrectAudience() {
            final JWTCreator.Builder mockedJwtBuilder = spy(JWTCreator.Builder.class);

            try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
                staticJwt.when(() -> JWT.create()).thenReturn(mockedJwtBuilder);

                serviceUnderTest.generateToken();

                verify(mockedJwtBuilder, times(1)).withAudience("test-audience");
            }
        }

        @Test
        void WithCorrectExpiration() {
            final JWTCreator.Builder mockedJwtBuilder = spy(JWTCreator.Builder.class);
            final long now = System.currentTimeMillis();

            try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
                staticJwt.when(() -> JWT.create()).thenReturn(mockedJwtBuilder);
                when(calendar.getTimeInMillis()).thenReturn(now);

                serviceUnderTest.generateToken();

                verify(mockedJwtBuilder, times(1)).withExpiresAt(new Date(now + 60 * 1000 * 60));
            }
        }

        @Test
        void WithCorrectSigningKey() {
            final JWTCreator.Builder mockedJwtBuilder = spy(JWTCreator.Builder.class);
            final Algorithm mockAlgorithm = mock(Algorithm.class);

            try (MockedStatic<Algorithm> staticAlgorithm = Mockito.mockStatic(Algorithm.class)) {
                try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
                    staticJwt.when(() -> JWT.create()).thenReturn(mockedJwtBuilder);
                    staticAlgorithm.when(() -> HMAC256("test-signing-key")).thenReturn(mockAlgorithm);
                    doReturn("a-custom-jwt").when(mockedJwtBuilder).sign(mockAlgorithm);

                    serviceUnderTest.generateToken();

                    staticAlgorithm.verify(() -> HMAC256("test-signing-key"), times(1));
                    verify(mockedJwtBuilder, times(1)).sign(mockAlgorithm);
                }
            }
        }

        @Test
        void ReturnsACustomJwt() {
            final JWTCreator.Builder mockedJwtBuilder = spy(JWTCreator.Builder.class);

            try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
                staticJwt.when(() -> JWT.create()).thenReturn(mockedJwtBuilder);
                doReturn("a-custom-jwt").when(mockedJwtBuilder).sign(any());

                final String response = serviceUnderTest.generateToken();

                assertThat(response).isEqualTo("a-custom-jwt");
            }
        }
    }
}