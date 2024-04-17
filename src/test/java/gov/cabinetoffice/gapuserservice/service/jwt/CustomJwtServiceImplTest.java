package gov.cabinetoffice.gapuserservice.service.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.Cache;
import gov.cabinetoffice.gapuserservice.config.JwtProperties;
import gov.cabinetoffice.gapuserservice.dto.JwtPayload;
import gov.cabinetoffice.gapuserservice.enums.LoginJourneyState;
import gov.cabinetoffice.gapuserservice.exceptions.UnauthorizedException;
import gov.cabinetoffice.gapuserservice.model.Role;
import gov.cabinetoffice.gapuserservice.model.RoleEnum;
import gov.cabinetoffice.gapuserservice.model.User;
import gov.cabinetoffice.gapuserservice.repository.JwtBlacklistRepository;
import gov.cabinetoffice.gapuserservice.repository.UserRepository;
import gov.cabinetoffice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import gov.cabinetoffice.gapuserservice.service.user.OneLoginUserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.WebUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.SignRequest;
import software.amazon.awssdk.services.kms.model.SignResponse;
import software.amazon.awssdk.services.kms.model.VerifyRequest;
import software.amazon.awssdk.services.kms.model.VerifyResponse;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class CustomJwtServiceImplTest {

    @InjectMocks
    private CustomJwtServiceImpl serviceUnderTest;

    @Mock
    private OneLoginUserService oneLoginUserService;

    @Mock
    private JwtBlacklistRepository jwtBlacklistRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private KmsClient kmsClient;

    @Captor
    private ArgumentCaptor<SignRequest> signRequestCaptor;

    private final String CHRISTMAS_2022_MIDDAY = "2022-12-25T12:00:00.00z";
    private final Clock clock = Clock.fixed(Instant.parse(CHRISTMAS_2022_MIDDAY), ZoneId.of("UTC"));
    private static MockedStatic<WebUtils> mockedWebUtils;

    @BeforeEach
    void setup() {
        mockedWebUtils = mockStatic(WebUtils.class);
        final JwtProperties jwtProperties = JwtProperties.builder()
                .signingKey("test-signing-key")
                .issuer("test-issuer")
                .audience("test-audience")
                .expiresAfter(60)
                .adminExpiresAfter(360)
                .build();
        serviceUnderTest = spy(new CustomJwtServiceImpl(
                oneLoginUserService, jwtProperties, jwtBlacklistRepository, userRepository, clock, kmsClient));
        ReflectionTestUtils.setField(serviceUnderTest, "userServiceCookieName", "userServiceCookieName");
        ReflectionTestUtils.setField(serviceUnderTest, "validateUserRolesInMiddleware", true);
    }

    @AfterEach
    public void close() {
        mockedWebUtils.close();
    }


    @Nested
    class IsTokenValid {
        final String jwt = "eyJraWQiOiIxODNjZTQ5YS0xYjExLTRiYjgtOWExMi1iYTViNzVmNGVmZjQiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOmliZDJyejJDZ3lpZG5kWHlxMnp5ZmNuUXd5WUk1N2gzNHZNbFNyODdDRGYiLCJhdWQiOiJGR1AiLCJpZFRva2VuIjoienFybiIsInJvbGVzIjoiW0FQUExJQ0FOVCwgRklORCwgQURNSU4sIFNVUEVSX0FETUlOXSIsImlzcyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MiIsImRlcGFydG1lbnQiOiJDYWJpbmV0IE9mZmljZSIsImV4cCI6MTY5MDQ1MjkzNCwiaWF0IjoxNjkwNDQ5MzM0LCJlbWFpbCI6InRlc3QudXNlckBnb3YudWsiLCJqdGkiOiIxOWQyMGYzMi0xZDIyLTQ0NzgtYjkwNi0wMzc5Mzg5ODUxODMifQ.H0xj5yob8u7eJs8zaCgjlTm_pt4fKrNhzFsmMcBNHghUaK3SQUoJcjUea31rKFppl5Dju90lq7n1fmNhVAk3Hpli6KvU2KNSgJZbCiMU5PbjJRZ3ogssjYjhOvor7fQrpComwZqA5-1I2jq9bQEIBwMyoSIpEJh1XFnk_GexedhlTY3ws0C9pX5gev7TL13cuc7xMRDRQE2G6-0sSOdTc5z0ib-Xjqz7D6tkLSOZJ4EKrbNnXpfar8KTSAUX6bO8Huj8q-FousOTpnlgOuUtNWXgdL928wP3fHgaCKsiALCDoHfoRSK2-0do0rsMmFHIrnIeZkJqvULgjgd55FLsAg";


        @Test
        void shouldNotCallValidateRolesInThePayloadWhenFlagIsDisabled() {
            ReflectionTestUtils.setField(serviceUnderTest, "validateUserRolesInMiddleware", false);

            when(kmsClient.verify(any(VerifyRequest.class))).thenReturn(VerifyResponse.builder()
                    .signatureValid(false)
                    .build());
            serviceUnderTest.isTokenValid(jwt);
            JwtPayload payload = new JwtPayload();
            Mockito.verify(serviceUnderTest, Mockito.never()).validateRolesInThePayload(payload);

        }

        @Test
        void ReturnsFalse_ifKmsVerificationFails() {
            ReflectionTestUtils.setField(serviceUnderTest, "oneLoginEnabled", true);
            when(kmsClient.verify(any(VerifyRequest.class))).thenReturn(VerifyResponse.builder()
                    .signatureValid(false)
                    .build());

            final boolean response = serviceUnderTest.isTokenValid(jwt);
            assertThat(response).isFalse();
            verify(kmsClient, times(1)).verify(any(VerifyRequest.class));
        }

        @Test
        void ReturnsFalse_IfBlacklisted() {
            ReflectionTestUtils.setField(serviceUnderTest, "oneLoginEnabled", true);

            when(jwtBlacklistRepository.existsByJwtIs(jwt)).thenReturn(true);
            User testUser = User.builder().roles(List.of(Role.builder().name(RoleEnum.FIND).id(1).build(),
                            Role.builder().name(RoleEnum.SUPER_ADMIN).id(4).build(),
                            Role.builder().name(RoleEnum.ADMIN).id(3).build(),
                            Role.builder().name(RoleEnum.APPLICANT).id(2).build()))
                    .loginJourneyState(LoginJourneyState.USER_READY).build();
            when(userRepository.findBySub(any())).thenReturn(Optional.of(testUser));
            when(oneLoginUserService.getUserBySub(any())).thenReturn(testUser);
            when(kmsClient.verify(any(VerifyRequest.class))).thenReturn(VerifyResponse.builder().signatureValid(true)
                    .build());

            final boolean response = serviceUnderTest.isTokenValid(jwt);

            assertThat(response).isFalse();
            verify(jwtBlacklistRepository, atLeastOnce()).existsByJwtIs(jwt);
        }

        @Test
        void ReturnsFalse_IfMemoizationFails() throws ExecutionException {
            ReflectionTestUtils.setField(serviceUnderTest, "oneLoginEnabled", true);

            Cache<String, Boolean> memoizationCache = mock(Cache.class);
            ReflectionTestUtils.setField(serviceUnderTest, "memoizationCache", memoizationCache);

            when(memoizationCache.get(any(), any())).thenThrow(new RuntimeException("Test execution exception"));

            final boolean response = serviceUnderTest.isTokenValid(jwt);

            assertThat(response).isFalse();
        }


        @Test
        void ReturnsTrue_IfValid() {
            ReflectionTestUtils.setField(serviceUnderTest, "oneLoginEnabled", true);

            User testUser = User.builder().roles(List.of(Role.builder().name(RoleEnum.FIND).id(1).build(),
                            Role.builder().name(RoleEnum.SUPER_ADMIN).id(4).build(),
                            Role.builder().name(RoleEnum.ADMIN).id(3).build(),
                            Role.builder().name(RoleEnum.APPLICANT).id(2).build()))
                    .loginJourneyState(LoginJourneyState.USER_READY).build();
            when(userRepository.findBySub(any())).thenReturn(Optional.of(
                    User.builder().loginJourneyState(LoginJourneyState.USER_READY).build()));
            when(userRepository.findBySub(any())).thenReturn(Optional.of(testUser));
            when(oneLoginUserService.getUserBySub(any())).thenReturn(testUser);

            when(kmsClient.verify(any(VerifyRequest.class))).thenReturn(VerifyResponse.builder().signatureValid(true)
                    .build());
            boolean response = serviceUnderTest.isTokenValid(jwt);

            verify(kmsClient, times(1)).verify(any(VerifyRequest.class));
            assertTrue(response);
        }
    }

    @Nested
    class GenerateToken {

        @Test
        void shouldReturnValidToken() throws JsonProcessingException {
            final Map<String, String> claims = new HashMap<>();
            claims.put("test-claim-key", "test-claim-value");

            when(kmsClient.sign(any(SignRequest.class))).thenReturn(SignResponse.builder()
                    .signature(SdkBytes.fromString("abc", StandardCharsets.UTF_8)).build());

            String response = serviceUnderTest.generateToken(claims, false);

            verify(kmsClient).sign(signRequestCaptor.capture());
            SignRequest capturedSignRequest = signRequestCaptor.getValue();
            String message = SdkBytes.fromByteArray(capturedSignRequest.message().asByteArray()).asString(StandardCharsets.UTF_8);

            assertEquals("RSASSA_PSS_SHA_256", capturedSignRequest.signingAlgorithm().name());
            assertEquals("RAW", capturedSignRequest.messageType().name());

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode jsonObject = objectMapper.readValue(message, ObjectNode.class);

            assertNotNull(response);
            assertEquals("test-audience", jsonObject.get("aud").asText());
            assertEquals("test-issuer", jsonObject.get("iss").asText());
            assertEquals("1671973200", jsonObject.get("exp").asText());
            assertEquals("test-claim-value", jsonObject.get("test-claim-key").asText());
            assertNotNull(jsonObject.get("iat").asText());
        }

        @Test
        void shouldReturnValidAdminToken() throws JsonProcessingException {
            final Map<String, String> claims = new HashMap<>();
            claims.put("test-claim-key", "test-claim-value");

            when(kmsClient.sign(any(SignRequest.class))).thenReturn(SignResponse.builder()
                    .signature(SdkBytes.fromString("abc", StandardCharsets.UTF_8)).build());

            String response = serviceUnderTest.generateToken(claims, true);

            verify(kmsClient).sign(signRequestCaptor.capture());
            SignRequest capturedSignRequest = signRequestCaptor.getValue();
            String message = SdkBytes.fromByteArray(capturedSignRequest.message().asByteArray()).asString(StandardCharsets.UTF_8);

            assertEquals("RSASSA_PSS_SHA_256", capturedSignRequest.signingAlgorithm().name());
            assertEquals("RAW", capturedSignRequest.messageType().name());

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode jsonObject = objectMapper.readValue(message, ObjectNode.class);

            assertNotNull(response);
            assertEquals("test-audience", jsonObject.get("aud").asText());
            assertEquals("test-issuer", jsonObject.get("iss").asText());
            assertEquals("1671991200", jsonObject.get("exp").asText());
            assertEquals("test-claim-value", jsonObject.get("test-claim-key").asText());
            assertNotNull(jsonObject.get("iat").asText());
        }

        @Nested
        class ValidateRolesInTheyPayload {
            @Test
            void testValidateRolesInThePayloadWithValidPayload() {
                User testUser = User.builder().gapUserId(1).sub("sub").build();
                JwtPayload payload = new JwtPayload();
                payload.setRoles("[FIND, APPLY]");
                when(oneLoginUserService.getUserBySub(any())).thenReturn(testUser);
                doNothing().when(oneLoginUserService).validateRoles(testUser.getRoles(), "[FIND, APPLY]");
                JwtPayload response = serviceUnderTest.validateRolesInThePayload(payload);

                assertThat(response).isSameAs(payload);
            }

            @Test
            void testValidateRolesInThePayloadWithInvalidPayload() {
                User testUser = User.builder().gapUserId(1).sub("sub").build();
                when(oneLoginUserService.getUserBySub(any())).thenReturn(testUser);
                JwtPayload payload = new JwtPayload();
                payload.setRoles("[FIND, APPLY]");
                doThrow(UnauthorizedException.class).when(oneLoginUserService)
                        .validateRoles(testUser.getRoles(), "[FIND, APPLY]");

                assertThrows(UnauthorizedException.class, () -> serviceUnderTest.validateRolesInThePayload(payload));
            }
        }

        @Nested
        class GetUserFromJwt {
            @Test
            void testGetUserFromJwtWithValidJwt() {
                String validJwt = "eyJraWQiOiIxODNjZTQ5YS0xYjExLTRiYjgtOWExMi1iYTViNzVmNGVmZjQiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOmliZDJyejJDZ3lpZG5kWHlxMnp5ZmNuUXd5WUk1N2gzNHZNbFNyODdDRGYiLCJhdWQiOiJGR1AiLCJpZFRva2VuIjoienFybiIsInJvbGVzIjoiW0FQUExJQ0FOVCwgRklORCwgQURNSU4sIFNVUEVSX0FETUlOXSIsImlzcyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MiIsImRlcGFydG1lbnQiOiJDYWJpbmV0IE9mZmljZSIsImV4cCI6MTY5MDQ1MjkzNCwiaWF0IjoxNjkwNDQ5MzM0LCJlbWFpbCI6InRlc3QudXNlckBnb3YudWsiLCJqdGkiOiIxOWQyMGYzMi0xZDIyLTQ0NzgtYjkwNi0wMzc5Mzg5ODUxODMifQ.H0xj5yob8u7eJs8zaCgjlTm_pt4fKrNhzFsmMcBNHghUaK3SQUoJcjUea31rKFppl5Dju90lq7n1fmNhVAk3Hpli6KvU2KNSgJZbCiMU5PbjJRZ3ogssjYjhOvor7fQrpComwZqA5-1I2jq9bQEIBwMyoSIpEJh1XFnk_GexedhlTY3ws0C9pX5gev7TL13cuc7xMRDRQE2G6-0sSOdTc5z0ib-Xjqz7D6tkLSOZJ4EKrbNnXpfar8KTSAUX6bO8Huj8q-FousOTpnlgOuUtNWXgdL928wP3fHgaCKsiALCDoHfoRSK2-0do0rsMmFHIrnIeZkJqvULgjgd55FLsAg";
                final MockHttpServletRequest request = new MockHttpServletRequest();
                User testUser = User.builder().gapUserId(1).sub("sub").build();
                Cookie cookie = new Cookie("userServiceCookieName", validJwt);
                mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                        .thenReturn(cookie);
                when(userRepository.findBySub(any())).thenReturn(Optional.of(testUser));
                Optional<User> response = serviceUnderTest.getUserFromJwt(request);

                assertThat(response).isEqualTo(Optional.of(testUser));
            }

            @Test
            void testGetUserFromJwtThrowsUnauthorizedWithNullJwt() {
                HttpServletRequest mockRequest = mock(HttpServletRequest.class);
                assertThrows(UnauthorizedException.class, () -> serviceUnderTest.getUserFromJwt(mockRequest));
            }

            @Test
            void testGetUserFromJwtWithNullValueInJwt() {
                HttpServletRequest mockRequest = mock(HttpServletRequest.class);
                when(WebUtils.getCookie(mockRequest, "userServiceCookieName")).thenReturn(null);
                assertThrows(UnauthorizedException.class, () -> serviceUnderTest.getUserFromJwt(mockRequest));
            }
        }
    }
}
