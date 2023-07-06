package gov.cabinetofice.gapuserservice.service;

import gov.cabinetofice.gapuserservice.dto.OneLoginUserInfoDto;
import gov.cabinetofice.gapuserservice.exceptions.*;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import static io.jsonwebtoken.impl.crypto.RsaProvider.generateKeyPair;
import static org.mockito.ArgumentMatchers.anyString;
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

    @BeforeEach
    void setUp() {

        mockedStatic = mockStatic(RestUtils.class);
        testKeyPair = generateTestKeys();
        ReflectionTestUtils.setField(oneLoginService, "privateKey", testKeyPair.get("private"));
        ReflectionTestUtils.setField(oneLoginService, "oneLoginBaseUrl", DUMMY_BASE_URL);
        ReflectionTestUtils.setField(oneLoginService, "clientAssertionType", "assertion_type");
        ReflectionTestUtils.setField(oneLoginService, "clientId", DUMMY_CLIENT_ID);
        ReflectionTestUtils.setField(oneLoginService, "serviceRedirectUrl", DUMMY_BASE_URL + "/redirect");

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

        String result = oneLoginService.getAuthToken("dummyJwt", "dummyCode");

        Assertions.assertEquals("dummyToken", result);
    }

    @Test
    void shouldReturnUserInfo() throws IOException, JSONException {
        String jsonResponse = "{\"sub\":\"urn:fdc:gov.uk:2022:jhkdasy7dal7dadhadasdas\"" +
                ",\"email_verified\":\"true\",\"email\":\"test.user@email.com\"}";
        OneLoginUserInfoDto expectedResponse = OneLoginUserInfoDto.builder()
                .sub("urn:fdc:gov.uk:2022:jhkdasy7dal7dadhadasdas")
                .email("test.user@email.com")
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
    void shouldThrowAuthenticationExceptionWhenAccessTokenIsInvalid() throws IOException, JSONException {

        String expectedResponse = "{\"error_description\":\"Invalid grant\",\"error\":\"invalid_grant\"}";

        String requestBody = "grant_type=" + GRANT_TYPE +
                "&code=" + "dummyCode" +
                "&redirect_uri=" + DUMMY_BASE_URL + "/redirect" +
                "&client_assertion_type=" + "assertion_type" +
                "&client_assertion=" + "dummyJwt";

        when(RestUtils.postRequestWithBody(DUMMY_BASE_URL + "/token",
                requestBody, "application/x-www-form-urlencoded"))
                .thenReturn(new JSONObject(expectedResponse));



        Assertions.assertThrows(AuthenticationException.class, () -> oneLoginService
                .getAuthToken("dummyJwt", "dummyCode"));
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
                .getAuthToken("dummyJwt", "dummyCode"));
    }

    @Test
    void shouldReturnDoesUserExist_Email() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        final boolean result = oneLoginService.doesUserExistByEmail("");

        Assertions.assertTrue(result);
    }

    @Test
    void shouldReturnDoesUserExist_Sub() {
        when(userRepository.existsBySub(anyString())).thenReturn(true);

        final boolean result = oneLoginService.doesUserExistBySub("");

        Assertions.assertTrue(result);
    }

    @Test
    void shouldReturnUsersRoles_Email() {
        final List<Role> roles = List.of(
                Role.builder().name(RoleEnum.APPLICANT).build(),
                Role.builder().name(RoleEnum.FIND).build(),
                Role.builder().name(RoleEnum.ADMIN).build(),
                Role.builder().name(RoleEnum.SUPER_ADMIN).build()
        );

        when(roleRepository.findByUsers_Email(anyString())).thenReturn(roles);

        final List<RoleEnum> result = oneLoginService.getUsersRolesByEmail("");

        Assertions.assertEquals(4, result.size());
        Assertions.assertEquals(RoleEnum.APPLICANT, result.get(0));
        Assertions.assertEquals(RoleEnum.FIND, result.get(1));
        Assertions.assertEquals(RoleEnum.ADMIN, result.get(2));
        Assertions.assertEquals(RoleEnum.SUPER_ADMIN, result.get(3));
    }

    @Test
    void shouldReturnUsersRoles_Sub() {
        final List<Role> roles = List.of(
                Role.builder().name(RoleEnum.APPLICANT).build(),
                Role.builder().name(RoleEnum.FIND).build()
        );

        when(roleRepository.findByUsers_Sub(anyString())).thenReturn(roles);

        final List<RoleEnum> result = oneLoginService.getUsersRolesBySub("");

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(RoleEnum.APPLICANT, result.get(0));
        Assertions.assertEquals(RoleEnum.FIND, result.get(1));
    }

    @Test
    void shouldReturnNewUserRoles() {
        final List<RoleEnum> result = oneLoginService.getNewUserRoles();

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(RoleEnum.APPLICANT, result.get(0));
        Assertions.assertEquals(RoleEnum.FIND, result.get(1));
    }

    @Nested
    class isUserApplicant {
        @Test
        void shouldReturnFalseWhenUserIsAdmin() {
            final List<RoleEnum> roles = List.of(RoleEnum.APPLICANT, RoleEnum.FIND, RoleEnum.ADMIN);

            final boolean response = oneLoginService.isUserAnApplicant(roles);

            Assertions.assertFalse(response);
        }

        @Test
        void shouldReturnFalseWhenUserIsSuperAdmin() {
            final List<RoleEnum> roles = List.of(RoleEnum.APPLICANT, RoleEnum.FIND, RoleEnum.SUPER_ADMIN, RoleEnum.ADMIN);

            final boolean response = oneLoginService.isUserAnApplicant(roles);

            Assertions.assertFalse(response);
        }

        @Test
        void shouldReturnFalseWhenUserIsNotAnApplicant() {
            final List<RoleEnum> roles = new ArrayList<>();

            final boolean response = oneLoginService.isUserAnApplicant(roles);

            Assertions.assertFalse(response);
        }

        @Test
        void shouldReturnTrueWhenUserIsApplicant() {
            final List<RoleEnum> roles = List.of(RoleEnum.APPLICANT, RoleEnum.FIND);

            final boolean response = oneLoginService.isUserAnApplicant(roles);

            Assertions.assertTrue(response);
        }
    }

    @Nested
    class isUserAdmin {
        @Test
        void shouldReturnTrueWhenUserIsAdmin() {
            final List<RoleEnum> roles = List.of(RoleEnum.APPLICANT, RoleEnum.FIND, RoleEnum.ADMIN);

            final boolean response = oneLoginService.isUserAnAdmin(roles);

            Assertions.assertTrue(response);
        }

        @Test
        void shouldReturnTrueWhenUserIsSuperAdmin() {
            final List<RoleEnum> roles = List.of(RoleEnum.APPLICANT, RoleEnum.FIND, RoleEnum.SUPER_ADMIN, RoleEnum.ADMIN);

            final boolean response = oneLoginService.isUserAnAdmin(roles);

            Assertions.assertTrue(response);
        }

        @Test
        void shouldReturnFalseWhenUserIsAnApplicant() {
            final List<RoleEnum> roles = List.of(RoleEnum.APPLICANT, RoleEnum.FIND);

            final boolean response = oneLoginService.isUserAnAdmin(roles);

            Assertions.assertFalse(response);
        }
    }

    @Nested
    class isUserSuperAdmin {
        @Test
        void shouldReturnFalseWhenUserIsAdmin() {
            final List<RoleEnum> roles = List.of(RoleEnum.APPLICANT, RoleEnum.FIND, RoleEnum.ADMIN);

            final boolean response = oneLoginService.isUserASuperAdmin(roles);

            Assertions.assertFalse(response);
        }

        @Test
        void shouldReturnTrueWhenUserIsSuperAdmin() {
            final List<RoleEnum> roles = List.of(RoleEnum.APPLICANT, RoleEnum.FIND, RoleEnum.SUPER_ADMIN, RoleEnum.ADMIN);

            final boolean response = oneLoginService.isUserASuperAdmin(roles);

            Assertions.assertTrue(response);
        }

        @Test
        void shouldReturnFalseWhenUserIsAnApplicant() {
            final List<RoleEnum> roles = List.of(RoleEnum.APPLICANT, RoleEnum.FIND);

            final boolean response = oneLoginService.isUserASuperAdmin(roles);

            Assertions.assertFalse(response);
        }
    }

    @Nested
    class createUser {
        @Test
        void shouldReturnUserRolesWhenUserIsCreated() {
            when(roleRepository.findByName(RoleEnum.APPLICANT)).thenReturn(Optional.of(Role.builder().name(RoleEnum.APPLICANT).build()));
            when(roleRepository.findByName(RoleEnum.FIND)).thenReturn(Optional.of(Role.builder().name(RoleEnum.FIND).build()));

            final List<RoleEnum> result = oneLoginService.createUser("", "");

            Assertions.assertEquals(2, result.size());
            Assertions.assertEquals(RoleEnum.APPLICANT, result.get(0));
            Assertions.assertEquals(RoleEnum.FIND, result.get(1));
        }

        @Test
        void shouldSaveUserWithSubAndEmailWhenUserIsCreated() {
            when(roleRepository.findByName(any())).thenReturn(Optional.of(Role.builder().name(RoleEnum.APPLICANT).build()));

            oneLoginService.createUser("sub", "test@email.com");

            final ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userArgumentCaptor.capture());
            Assertions.assertEquals("sub", userArgumentCaptor.getValue().getSub());
            Assertions.assertEquals("test@email.com", userArgumentCaptor.getValue().getEmail());
        }

        @Test
        void shouldThrowExceptionWhenRoleDoesNotExist() {
            when(roleRepository.findByName(any())).thenReturn(Optional.empty());

            Assertions.assertThrows(RoleNotFoundException.class, () -> oneLoginService.createUser("", ""));
        }
    }

    @Nested
    class addSubToUser {
        @Test
        void shouldSaveUserWithSubWhenUserExists() {
            final String email = "email@test.com";
            final User user = User.builder().id(1).email(email).build();

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            oneLoginService.addSubToUser("sub", email);

            final ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userArgumentCaptor.capture());
            Assertions.assertEquals("sub", userArgumentCaptor.getValue().getSub());
        }

        @Test
        void shouldThrowExceptionWhenUserDoesNotExist() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            Assertions.assertThrows(UserNotFoundException.class, () -> oneLoginService.addSubToUser("", ""));
        }
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
