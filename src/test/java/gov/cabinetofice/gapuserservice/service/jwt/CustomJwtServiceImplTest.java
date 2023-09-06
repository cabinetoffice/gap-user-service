package gov.cabinetofice.gapuserservice.service.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Verification;
import com.nimbusds.jose.JOSEException;
import gov.cabinetofice.gapuserservice.config.JwtProperties;
import gov.cabinetofice.gapuserservice.enums.LoginJourneyState;
import gov.cabinetofice.gapuserservice.mappers.RoleMapper;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.repository.JwtBlacklistRepository;
import gov.cabinetofice.gapuserservice.repository.UserRepository;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import gov.cabinetofice.gapuserservice.service.user.OneLoginUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static com.auth0.jwt.JWT.decode;
import static com.auth0.jwt.JWT.require;
import static com.auth0.jwt.algorithms.Algorithm.RSA256;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class CustomJwtServiceImplTest {

    @InjectMocks
    private CustomJwtServiceImpl serviceUnderTest;

    @Mock
    private JwtBlacklistRepository jwtBlacklistRepository;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private OneLoginUserService oneLoginUserService;

    @Mock
    private JwtProperties jwtProperties;

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

        serviceUnderTest = spy(new CustomJwtServiceImpl(roleMapper, oneLoginUserService, jwtProperties, jwtBlacklistRepository, userRepository, clock));
    }

    @Nested
    class IsTokenValid {
        @Mock
        private final JWTVerifier mockedJwtVerifier = mock(JWTVerifier.class);
        final Algorithm mockAlgorithm = mock(Algorithm.class);
        private final Verification verification = JWT.require(mockAlgorithm);

        final String jwt = "eyJraWQiOiIxODNjZTQ5YS0xYjExLTRiYjgtOWExMi1iYTViNzVmNGVmZjQiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOmliZDJyejJDZ3lpZG5kWHlxMnp5ZmNuUXd5WUk1N2gzNHZNbFNyODdDRGYiLCJhdWQiOiJGR1AiLCJpZFRva2VuIjoienFybiIsInJvbGVzIjoiW0FQUExJQ0FOVCwgRklORCwgQURNSU4sIFNVUEVSX0FETUlOXSIsImlzcyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MiIsImRlcGFydG1lbnQiOiJDYWJpbmV0IE9mZmljZSIsImV4cCI6MTY5MDQ1MjkzNCwiaWF0IjoxNjkwNDQ5MzM0LCJlbWFpbCI6InRlc3QudXNlckBnb3YudWsiLCJqdGkiOiIxOWQyMGYzMi0xZDIyLTQ0NzgtYjkwNi0wMzc5Mzg5ODUxODMifQ.H0xj5yob8u7eJs8zaCgjlTm_pt4fKrNhzFsmMcBNHghUaK3SQUoJcjUea31rKFppl5Dju90lq7n1fmNhVAk3Hpli6KvU2KNSgJZbCiMU5PbjJRZ3ogssjYjhOvor7fQrpComwZqA5-1I2jq9bQEIBwMyoSIpEJh1XFnk_GexedhlTY3ws0C9pX5gev7TL13cuc7xMRDRQE2G6-0sSOdTc5z0ib-Xjqz7D6tkLSOZJ4EKrbNnXpfar8KTSAUX6bO8Huj8q-FousOTpnlgOuUtNWXgdL928wP3fHgaCKsiALCDoHfoRSK2-0do0rsMmFHIrnIeZkJqvULgjgd55FLsAg";


        @Test
        void ReturnsTrue_IfValid() {
            final Algorithm mockAlgorithm = mock(Algorithm.class);
            final Verification spiedVerification = spy(verification);
            ReflectionTestUtils.setField(serviceUnderTest, "oneLoginEnabled", true);

            try (MockedStatic<Algorithm> staticAlgorithm = Mockito.mockStatic(Algorithm.class)) {
                staticAlgorithm.when(() -> RSA256(any(), any())).thenReturn(mockAlgorithm);
                try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
                    staticJwt.when(() -> require(any())).thenReturn(spiedVerification);
                    staticJwt.when(() -> decode(any())).thenCallRealMethod();
                    when(spiedVerification.build()).thenReturn(mockedJwtVerifier);
                    User testUser = User.builder().roles(List.of(Role.builder().name(RoleEnum.FIND).id(1).build(), Role.builder().name(RoleEnum.SUPER_ADMIN).id(4).build(), Role.builder().name(RoleEnum.ADMIN).id(3).build(), Role.builder().name(RoleEnum.APPLICANT).id(2).build())).loginJourneyState(LoginJourneyState.USER_READY).build();
                    when(userRepository.findBySub(any())).thenReturn(Optional.of(testUser));
                    when(oneLoginUserService.getUserBySub(any())).thenReturn(testUser);
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
            ReflectionTestUtils.setField(serviceUnderTest, "oneLoginEnabled", true);

            try (MockedStatic<Algorithm> staticAlgorithm = Mockito.mockStatic(Algorithm.class)) {
                staticAlgorithm.when(() -> RSA256(any(), any())).thenReturn(mockAlgorithm);
                try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
                    staticJwt.when(() -> require(any())).thenReturn(spiedVerification);
                    staticJwt.when(() -> decode(any())).thenCallRealMethod();
                    when(spiedVerification.build()).thenReturn(mockedJwtVerifier);
                    when(jwtBlacklistRepository.existsByJwtIs(jwt)).thenReturn(true);
                    User testUser = User.builder().roles(List.of(Role.builder().name(RoleEnum.FIND).id(1).build(), Role.builder().name(RoleEnum.SUPER_ADMIN).id(4).build(), Role.builder().name(RoleEnum.ADMIN).id(3).build(), Role.builder().name(RoleEnum.APPLICANT).id(2).build())).loginJourneyState(LoginJourneyState.USER_READY).build();
                    when(userRepository.findBySub(any())).thenReturn(Optional.of(testUser));
                    when(oneLoginUserService.getUserBySub(any())).thenReturn(testUser);

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
            ReflectionTestUtils.setField(serviceUnderTest, "oneLoginEnabled", true);

            try (MockedStatic<Algorithm> staticAlgorithm = Mockito.mockStatic(Algorithm.class)) {
                try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
                    staticJwt.when(() -> require(any())).thenReturn(spiedVerification);
                    staticJwt.when(() -> decode(any())).thenCallRealMethod();
                    when(spiedVerification.build()).thenReturn(mockedJwtVerifier);
                    staticAlgorithm.when(() -> RSA256(any(), any())).thenReturn(mockAlgorithm);
                    User testUser = User.builder().roles(List.of(Role.builder().name(RoleEnum.FIND).id(1).build(), Role.builder().name(RoleEnum.SUPER_ADMIN).id(4).build(), Role.builder().name(RoleEnum.ADMIN).id(3).build(), Role.builder().name(RoleEnum.APPLICANT).id(2).build())).loginJourneyState(LoginJourneyState.USER_READY).build();
                    when(userRepository.findBySub(any())).thenReturn(Optional.of(User.builder().loginJourneyState(LoginJourneyState.USER_READY).build()));
                    when(userRepository.findBySub(any())).thenReturn(Optional.of(testUser));
                    when(oneLoginUserService.getUserBySub(any())).thenReturn(testUser);
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
