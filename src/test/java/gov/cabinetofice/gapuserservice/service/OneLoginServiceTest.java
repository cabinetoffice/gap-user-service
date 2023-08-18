package gov.cabinetofice.gapuserservice.service;

import gov.cabinetofice.gapuserservice.dto.MigrateUserDto;
import gov.cabinetofice.gapuserservice.dto.IdTokenDto;
import gov.cabinetofice.gapuserservice.dto.OneLoginUserInfoDto;
import gov.cabinetofice.gapuserservice.dto.StateCookieDto;
import gov.cabinetofice.gapuserservice.enums.LoginJourneyState;
import gov.cabinetofice.gapuserservice.exceptions.*;
import gov.cabinetofice.gapuserservice.model.Department;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.repository.RoleRepository;
import gov.cabinetofice.gapuserservice.repository.UserRepository;
import gov.cabinetofice.gapuserservice.util.RestUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.apache.http.HttpHeaders;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import static io.jsonwebtoken.impl.crypto.RsaProvider.generateKeyPair;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OneLoginServiceTest {

    @InjectMocks
    private OneLoginService oneLoginService;

    private static MockedStatic<RestUtils> mockedStatic;

    private Map<String, String> testKeyPair;

    private static final String DUMMY_CLIENT_ID = "asdhjlsadfbfds";
    private static final String DUMMY_BASE_URL = "https://test.url.gov";
    private static final String GRANT_TYPE = "authorization_code";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @BeforeEach
    void setUp() {
        mockedStatic = mockStatic(RestUtils.class);
        testKeyPair = generateTestKeys();
        ReflectionTestUtils.setField(oneLoginService, "privateKey", testKeyPair.get("private"));
        ReflectionTestUtils.setField(oneLoginService, "oneLoginBaseUrl", DUMMY_BASE_URL);
        ReflectionTestUtils.setField(oneLoginService, "clientAssertionType", "assertion_type");
        ReflectionTestUtils.setField(oneLoginService, "clientId", DUMMY_CLIENT_ID);
        ReflectionTestUtils.setField(oneLoginService, "serviceRedirectUrl", DUMMY_BASE_URL + "/redirect");
        ReflectionTestUtils.setField(oneLoginService, "adminBackend", "adminBackend");
    }

    @AfterEach
    public void close() {
        mockedStatic.close();
    }


    @Test
    void shouldReturnValidJwtToken() throws NoSuchAlgorithmException, InvalidKeySpecException {
        String result = oneLoginService.createOneLoginJwt();

        Claims claims = getClaims(result);

        Assertions.assertEquals(DUMMY_BASE_URL + "/token", claims.getAudience());
        Assertions.assertEquals(DUMMY_CLIENT_ID, claims.getIssuer());
        Assertions.assertEquals(DUMMY_CLIENT_ID, claims.getSubject());
        Assertions.assertNotNull(claims.getExpiration());
        Assertions.assertNotNull(claims.get("exp"));
        Assertions.assertNotNull(claims.get("jti"));
        Assertions.assertNotNull(claims.get("iat"));
    }

    @Test
    void shouldThrowPrivateKeyParsingExceptionWhenKeyIsInvalid() {
        ReflectionTestUtils.setField(oneLoginService, "privateKey", "invalidKey");

        Assertions.assertThrows(PrivateKeyParsingException.class, () -> oneLoginService.createOneLoginJwt());
    }


    @Test
    void shouldReturnValidAuthToken() throws IOException, JSONException {
        String requestBody = "grant_type=" + GRANT_TYPE +
                "&code=" + "dummyCode" +
                "&redirect_uri=" + DUMMY_BASE_URL + "/redirect" +
                "&client_assertion_type=" + "assertion_type" +
                "&client_assertion=" + "dummyJwt";

        when(RestUtils.postRequestWithBody(DUMMY_BASE_URL + "/token",
                requestBody, "application/x-www-form-urlencoded"))
                .thenReturn(new JSONObject("{\"access_token\":\"dummyToken\"" +
                ",\"token_type\":\"Bearer\",\"expires_in\":180}"));

        String result = oneLoginService.getTokenResponse("dummyJwt", "dummyCode").getString("access_token");

        Assertions.assertEquals("dummyToken", result);
    }

    @Test
    void shouldReturnUserInfo() throws IOException, JSONException {
        String jsonResponse = "{\"sub\":\"urn:fdc:gov.uk:2022:jhkdasy7dal7dadhadasdas\"" +
                ",\"email_verified\":\"true\",\"email\":\"test.user@email.com\"}";
        OneLoginUserInfoDto expectedResponse = OneLoginUserInfoDto.builder()
                .sub("urn:fdc:gov.uk:2022:jhkdasy7dal7dadhadasdas")
                .emailAddress("test.user@email.com")
                .build();

       Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + "accessToken");

        when(RestUtils.getRequestWithHeaders(DUMMY_BASE_URL + "/userinfo", headers))
                .thenReturn(new JSONObject(jsonResponse));

        OneLoginUserInfoDto result = oneLoginService.getUserInfo("accessToken");

        Assertions.assertEquals(expectedResponse, result);
    }

    @Test
    void shouldThrowAuthenticationExceptionWhenRequestFails() throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + "accessToken");

        when(RestUtils.getRequestWithHeaders(DUMMY_BASE_URL + "/userinfo", headers))
                .thenThrow(new IOException());


        Assertions.assertThrows(AuthenticationException.class, () -> oneLoginService
                .getUserInfo("accessToken"));
    }

    @Test
    void shouldThrowInvalidRequestExceptionWhenInvalidUrlIsGiven() throws IOException {

        String requestBody = "grant_type=" + GRANT_TYPE +
                "&code=" + "dummyCode" +
                "&redirect_uri=" + DUMMY_BASE_URL + "/redirect" +
                "&client_assertion_type=" + "assertion_type" +
                "&client_assertion=" + "dummyJwt";

        when(RestUtils.postRequestWithBody(DUMMY_BASE_URL + "/token",
                requestBody, "application/x-www-form-urlencoded"))
                .thenThrow(new IOException());

        Assertions.assertThrows(InvalidRequestException.class, () -> oneLoginService
                .getTokenResponse("dummyJwt", "dummyCode"));
    }

    @Test
    void shouldReturnNewUserRoles() {
        final List<RoleEnum> result = oneLoginService.getNewUserRoles();

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(RoleEnum.APPLICANT, result.get(0));
        Assertions.assertEquals(RoleEnum.FIND, result.get(1));
    }

    @Nested
    class createUser {
        @Test
        void shouldReturnSavedUser() {
            when(roleRepository.findByName(any())).thenReturn(Optional.of(Role.builder().name(RoleEnum.APPLICANT).build()));
            when(userRepository.save(any())).thenReturn(User.builder().roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build())).build());

            final User result = oneLoginService.createNewUser("", "");

            Assertions.assertEquals(1, result.getRoles().size());
            Assertions.assertEquals(RoleEnum.APPLICANT, result.getRoles().get(0).getName());
        }

        @Test
        void shouldSaveUserWithSubAndEmailWhenUserIsCreated() {
            when(roleRepository.findByName(any())).thenReturn(Optional.of(Role.builder().name(RoleEnum.APPLICANT).build()));

            oneLoginService.createNewUser("sub", "test@email.com");

            final ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userArgumentCaptor.capture());
            Assertions.assertEquals("sub", userArgumentCaptor.getValue().getSub());
            Assertions.assertEquals("test@email.com", userArgumentCaptor.getValue().getEmailAddress());
        }

        @Test
        void shouldThrowExceptionWhenRoleDoesNotExist() {
            when(roleRepository.findByName(any())).thenReturn(Optional.empty());

            Assertions.assertThrows(RoleNotFoundException.class, () -> oneLoginService.createNewUser("", ""));
        }
    }

    @Nested
    class setUsersLoginJourneyState {
        @Test
        void shouldSetUsersLoginJourneyState() {
            final User user = User.builder()
                    .loginJourneyState(LoginJourneyState.PRIVACY_POLICY_PENDING)
                    .build();

            oneLoginService.setUsersLoginJourneyState(user, LoginJourneyState.PRIVACY_POLICY_ACCEPTED);

            final ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userArgumentCaptor.capture());
            assertThat(userArgumentCaptor.getValue().getLoginJourneyState()).isEqualTo(LoginJourneyState.PRIVACY_POLICY_ACCEPTED);
        }
    }

    @Nested
    class generateCustomJwtClaims {

        final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                .sub("sub")
                .emailAddress("email")
                .build();
        final User.UserBuilder userBuilder = User.builder()
                .sub("sub")
                .emailAddress("email")
                .roles(List.of(
                        Role.builder().name(RoleEnum.APPLICANT).build(),
                        Role.builder().name(RoleEnum.FIND).build()
                ))
                .department(Department.builder().name("department").build());

        @Test
        void shouldAddEmailAndSub() {
            final User user = userBuilder.build();
            when(userRepository.findBySub(any())).thenReturn(Optional.of(user));

            final Map<String, String> result = oneLoginService.generateCustomJwtClaims(oneLoginUserInfoDto);

            Assertions.assertEquals("sub", result.get("sub"));
            Assertions.assertEquals("email", result.get("email"));
        }

        @Test
        void shouldAddRoles() {
            final User user = userBuilder.build();
            when(userRepository.findBySub(any())).thenReturn(Optional.of(user));

            final Map<String, String> result = oneLoginService.generateCustomJwtClaims(oneLoginUserInfoDto);

            Assertions.assertEquals("[APPLICANT, FIND]", result.get("roles"));
        }

        @Test
        void shouldAddDepartment() {
            final User user = userBuilder.build();
            when(userRepository.findBySub(any())).thenReturn(Optional.of(user));

            final Map<String, String> result = oneLoginService.generateCustomJwtClaims(oneLoginUserInfoDto);

            Assertions.assertEquals("department", result.get("department"));
        }

        @Test
        void shouldNotAddDepartment() {
            final User user = userBuilder.department(null).build();
            when(userRepository.findBySub(any())).thenReturn(Optional.of(user));

            final Map<String, String> result = oneLoginService.generateCustomJwtClaims(oneLoginUserInfoDto);

            Assertions.assertNull(result.get("department"));
        }

        @Test
        void shouldThrowUserNotFoundException_whenUserNotFound() {
            when(userRepository.findBySub(any())).thenReturn(Optional.empty());

            Assertions.assertThrows(UserNotFoundException.class, () -> oneLoginService.generateCustomJwtClaims(oneLoginUserInfoDto));
        }
    }

    @Nested
    class createOrGetUserFromInfo {
        @Test
        void getExistingUser() {
            final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                    .sub("sub")
                    .emailAddress("email")
                    .build();
            final User existingUser = User.builder().sub("sub").build();

            when(userRepository.findBySub(any())).thenReturn(Optional.of(existingUser));

            final User result = oneLoginService.createOrGetUserFromInfo(oneLoginUserInfoDto);

            Assertions.assertEquals(existingUser, result);
        }

        @Test
        void createNewUser() {
            final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                    .sub("sub")
                    .emailAddress("email")
                    .build();

            when(userRepository.findBySub(any())).thenReturn(Optional.empty());
            when(roleRepository.findByName(any())).thenReturn(Optional.of(Role.builder().name(RoleEnum.APPLICANT).build()));

            final User newUser = oneLoginService.createNewUser("sub", "email");
            final User result = oneLoginService.createOrGetUserFromInfo(oneLoginUserInfoDto);

            Assertions.assertEquals(newUser, result);
        }
    }

    @Nested
    class migrateUser {
        @Test
        void shouldMigrateUser() {
            final User user = User.builder()
                    .colaSub(UUID.randomUUID())
                    .sub("oneLoginSub")
                    .build();
            final MigrateUserDto migrateUserDto = MigrateUserDto.builder()
                    .oneLoginSub(user.getSub())
                    .colaSub(user.getColaSub())
                    .build();

            // TODO not sure how to spy/mock the builder pattern well. If anyone has a better way gimme a shout!
            final WebClient mockWebClient = mock(WebClient.class);
            final WebClient.RequestBodyUriSpec mockRequestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
            final WebClient.RequestBodySpec mockRequestBodySpec = mock(WebClient.RequestBodySpec.class);
            final WebClient.RequestHeadersSpec mockRequestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
            final WebClient.ResponseSpec mockResponseSpec = mock(WebClient.ResponseSpec.class);

            when(webClientBuilder.build()).thenReturn(mockWebClient);
            when(mockWebClient.patch()).thenReturn(mockRequestBodyUriSpec);
            when(mockRequestBodyUriSpec.uri(anyString())).thenReturn(mockRequestBodySpec);
            when(mockRequestBodySpec.header(anyString(), anyString())).thenReturn(mockRequestBodySpec);
            when(mockRequestBodySpec.contentType(any())).thenReturn(mockRequestBodySpec);
            when(mockRequestBodySpec.bodyValue(any())).thenReturn(mockRequestHeadersSpec);
            when(mockRequestHeadersSpec.retrieve()).thenReturn(mockResponseSpec);
            when(mockResponseSpec.bodyToMono(Void.class)).thenReturn(mock(Mono.class));

            oneLoginService.migrateUser(user, "jwt");

            verify(webClientBuilder).build();
            verify(mockRequestBodyUriSpec).uri("adminBackend/users/migrate");
            verify(mockRequestBodySpec).header("Authorization", "Bearer jwt");
            verify(mockRequestBodySpec).bodyValue(migrateUserDto);
        }
    }

    @Test
    void generateStateJson() {
        final String redirectUrl = "redirectUrl";
        final String state = "state";
        final String expected = "eyJyZWRpcmVjdFVybCI6InJlZGlyZWN0VXJsIiwic3RhdGUiOiJzdGF0ZSJ9";
        final String result = oneLoginService.buildEncodedStateJson(redirectUrl, state);
        Assertions.assertEquals(result, expected);
    }

    @Test
    void decodeStateCookie() {
        final String encodedStateCookie = "eyJyZWRpcmVjdFVybCI6InJlZGlyZWN0VXJsIiwic3RhdGUiOiJzdGF0ZSJ9";
        final StateCookieDto.StateCookieDtoBuilder stateCookieDtoBuilder = StateCookieDto.builder()
                .state("state")
                .redirectUrl("redirectUrl");
        final StateCookieDto expected = stateCookieDtoBuilder.build();
        final StateCookieDto result = oneLoginService.decodeStateCookie(encodedStateCookie);
        Assertions.assertEquals(result, expected);
    }

    @Test
    void getDecodedIdToken() throws JSONException {
        final IdTokenDto.IdTokenDtoBuilder idTokenDtoBuilder = IdTokenDto.builder().nonce("nonce");
        final JSONObject tokenResponse = new JSONObject();
        tokenResponse.put("id_token", "eyJraWQiOiI2NDRhZjU5OGI3ODBmNTQxMDZjYTBmM2MwMTczNDFiYzIzMGM0ZjgzNzNmMzVmMzJlMThlM2U0MGNjN2FjZmY2IiwiYWxnIjoiRVMyNTYifQ.eyJzdWIiOm51bGwsImF0X2hhc2giOm51bGwsImlzcyI6bnVsbCwiYXVkIjpudWxsLCJleHAiOjAsImlhdCI6MCwidm90IjpudWxsLCJ2dG0iOm51bGwsInNpZCI6bnVsbCwibm9uY2UiOiJub25jZSJ9.AvCEdn3oHfwQoMtf8xgYZ0vfeNi1ELuT5Egndb2M1njBgRSuZsOgFPrHLzTkeT_XYjqI06J48MtI9q-inpQ3Ag");
        final IdTokenDto expected = idTokenDtoBuilder.build();
        final IdTokenDto result = oneLoginService.getDecodedIdToken(tokenResponse);
        Assertions.assertEquals(expected, result);
    }

    private Claims getClaims(String jwtToken) throws NoSuchAlgorithmException, InvalidKeySpecException {

            byte[] publicKeyBytes = Base64.getDecoder().decode(testKeyPair.get("public"));
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            return Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(jwtToken).getBody();

    }
    private Map<String, String> generateTestKeys() {
        KeyPair keyPair = generateKeyPair();

        Map<String, String> keyPairMap = new HashMap<>();

        // Get the private key
        PrivateKey privateKey = keyPair.getPrivate();
        byte[] privateKeyBytes = privateKey.getEncoded();
        String privateKeyString = Base64.getEncoder().encodeToString(privateKeyBytes);
        keyPairMap.put("private", privateKeyString);

        // Get the public key
        PublicKey publicKey = keyPair.getPublic();
        byte[] publicKeyBytes = publicKey.getEncoded();
        String publicKeyString = Base64.getEncoder().encodeToString(publicKeyBytes);
        keyPairMap.put("public", publicKeyString);

        return keyPairMap;
    }
}
