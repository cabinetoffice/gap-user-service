package gov.cabinetofice.gapuserservice.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import gov.cabinetofice.gapuserservice.dto.IdTokenDto;
import gov.cabinetofice.gapuserservice.dto.JwtPayload;
import gov.cabinetofice.gapuserservice.dto.OneLoginUserInfoDto;
import gov.cabinetofice.gapuserservice.dto.StateCookieDto;
import gov.cabinetofice.gapuserservice.exceptions.*;
import gov.cabinetofice.gapuserservice.model.*;
import gov.cabinetofice.gapuserservice.repository.RoleRepository;
import gov.cabinetofice.gapuserservice.repository.UserRepository;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import gov.cabinetofice.gapuserservice.service.user.OneLoginUserService;
import gov.cabinetofice.gapuserservice.util.LoggingUtils;
import gov.cabinetofice.gapuserservice.util.RestUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static io.jsonwebtoken.impl.crypto.RsaProvider.generateKeyPair;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OneLoginServiceTest {

    @InjectMocks
    private OneLoginService oneLoginService;
    @Mock
    private JwtBlacklistService jwtBlacklistService;

    @Mock
    private CustomJwtServiceImpl customJwtService;

    @Mock
    private LoggingUtils loggingUtils;

    @Mock
    private OneLoginUserService oneLoginUserService;

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
        ReflectionTestUtils.setField(oneLoginService, "mfaEnabled", true);
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

        assertThrows(PrivateKeyParsingException.class, () -> oneLoginService.createOneLoginJwt());
    }


    @Test
    void shouldReturnValidAuthToken() throws IOException, JSONException {
        String requestBody = "grant_type=" + GRANT_TYPE +
                "&code=" + "dummyCode" +
                "&redirect_uri=" + DUMMY_BASE_URL + "/redirect" +
                "&client_assertion_type=" + "assertion_type" +
                "&client_assertion=" + "dummyJwt";

        JSONObject expected = new JSONObject("{\"access_token\":\"dummyToken\"" +
                ",\"token_type\":\"Bearer\",\"expires_in\":180}");

        when(RestUtils.postRequestWithBody(DUMMY_BASE_URL + "/token",
                requestBody, "application/x-www-form-urlencoded"))
                .thenReturn(expected);

        JSONObject result = oneLoginService.getTokenResponse("dummyJwt", "dummyCode");

        Assertions.assertEquals( result, expected);
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


        assertThrows(AuthenticationException.class, () -> oneLoginService
                .getUserInfo("accessToken"));
    }


    @Test
    void shouldReturnErrorWhenAccessTokenIsInvalid() throws IOException, JSONException {

        String expectedResponse = "{\"error_description\":\"Invalid grant\",\"error\":\"invalid_grant\"}";

        String requestBody = "grant_type=" + GRANT_TYPE +
                "&code=" + "dummyCode" +
                "&redirect_uri=" + DUMMY_BASE_URL + "/redirect" +
                "&client_assertion_type=" + "assertion_type" +
                "&client_assertion=" + "dummyJwt";

        when(RestUtils.postRequestWithBody(DUMMY_BASE_URL + "/token",
                requestBody, "application/x-www-form-urlencoded"))
                .thenReturn(new JSONObject(expectedResponse));

        JSONObject jsonResponse = oneLoginService
                .getTokenResponse("dummyJwt", "dummyCode");
        JSONObject jsonExpectedResponse = new JSONObject(expectedResponse);

        Assertions.assertEquals(jsonResponse.getString("error_description"), jsonExpectedResponse.getString("error_description"));
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

        assertThrows(InvalidRequestException.class, () -> oneLoginService
                .getTokenResponse("dummyJwt", "dummyCode"));
    }

    @Nested
    class GenerateCustomJwtClaimsTest {

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
            when(oneLoginUserService.getUserFromSub(any())).thenReturn(Optional.of(user));

            final Map<String, String> result = oneLoginService.generateCustomJwtClaims(oneLoginUserInfoDto, "tokenHint");

            Assertions.assertEquals("sub", result.get("sub"));
            Assertions.assertEquals("email", result.get("email"));
        }

        @Test
        void shouldAddRoles() {
            final User user = userBuilder.build();
            when(oneLoginUserService.getUserFromSub(any())).thenReturn(Optional.of(user));

            final Map<String, String> result = oneLoginService.generateCustomJwtClaims(oneLoginUserInfoDto, "tokenHint");

            Assertions.assertEquals("[APPLICANT, FIND]", result.get("roles"));
        }

        @Test
        void shouldAddDepartment() {
            final User user = userBuilder.build();
            when(oneLoginUserService.getUserFromSub(any())).thenReturn(Optional.of(user));

            final Map<String, String> result = oneLoginService.generateCustomJwtClaims(oneLoginUserInfoDto, "tokenHint");

            Assertions.assertEquals("department", result.get("department"));
        }

        @Test
        void shouldNotAddDepartment() {
            final User user = userBuilder.department(null).build();
            when(oneLoginUserService.getUserFromSub(any())).thenReturn(Optional.of(user));

            final Map<String, String> result = oneLoginService.generateCustomJwtClaims(oneLoginUserInfoDto, "tokenHint");

            Assertions.assertNull(result.get("department"));
        }

        @Test
        void shouldThrowUserNotFoundException_whenUserNotFound() {
            assertThrows(UserNotFoundException.class, () -> oneLoginService.generateCustomJwtClaims(oneLoginUserInfoDto, "tokenHint"));
        }
    }

    @Test
    void testLogoutUser() throws IOException, JSONException {
        String tokenValue = "token.in.threeParts";
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        DecodedJWT decodedJWT = mock(DecodedJWT.class);
        JwtPayload payload = new JwtPayload();
        payload.setIdToken(tokenValue);

        when(customJwtService.decodedJwt(tokenValue)).thenReturn(decodedJWT);
        when(customJwtService.decodeTheTokenPayloadInAReadableFormat(decodedJWT)).thenReturn(payload);
        when(RestUtils.getRequest(any())).thenReturn(httpResponse);
        Cookie customJWTCookie = new Cookie("customJWT", tokenValue);

        oneLoginService.logoutUser(customJWTCookie, response);

        verify(customJwtService, times(1)).decodedJwt(tokenValue);
        verify(customJwtService, times(1)).decodeTheTokenPayloadInAReadableFormat(decodedJWT);
    }

    @Test
    void generateStateJson() {
        final String redirectUrl = "redirectUrl";
        final String state = "state";
        final String saltId = "saltId";
        final String expected = "eyJyZWRpcmVjdFVybCI6InJlZGlyZWN0VXJsIiwic2FsdElkIjoic2FsdElkIiwic3RhdGUiOiJzdGF0ZSJ9";
        final String result = oneLoginService.buildEncodedStateJson(redirectUrl, state, saltId);
        Assertions.assertEquals(result, expected);
    }

    @Test
    void decodeStateCookie() {
        final String encodedStateCookie = "eyJyZWRpcmVjdFVybCI6InJlZGlyZWN0VXJsIiwic2FsdElkIjoic2FsdElkIiwic3RhdGUiOiJzdGF0ZSJ9";
        final StateCookieDto.StateCookieDtoBuilder stateCookieDtoBuilder = StateCookieDto.builder()
                .state("state")
                .redirectUrl("redirectUrl")
                .saltId("saltId");
        final StateCookieDto expected = stateCookieDtoBuilder.build();
        final StateCookieDto result = oneLoginService.decodeStateCookie(encodedStateCookie);
        Assertions.assertEquals(result, expected);
    }

    @Test
    void getDecodedIdToken() throws JSONException {
        final IdTokenDto.IdTokenDtoBuilder idTokenDtoBuilder = IdTokenDto.builder().nonce("nonce");
        final JSONObject tokenResponse = new JSONObject();
        String idToken = "eyJraWQiOiI2NDRhZjU5OGI3ODBmNTQxMDZjYTBmM2MwMTczNDFiYzIzMGM0ZjgzNzNmMzVmMzJlMThlM2U0MGNjN2FjZmY2IiwiYWxnIjoiRVMyNTYifQ.eyJzdWIiOm51bGwsImF0X2hhc2giOm51bGwsImlzcyI6bnVsbCwiYXVkIjpudWxsLCJleHAiOjAsImlhdCI6MCwidm90IjpudWxsLCJ2dG0iOm51bGwsInNpZCI6bnVsbCwibm9uY2UiOiJub25jZSJ9.AvCEdn3oHfwQoMtf8xgYZ0vfeNi1ELuT5Egndb2M1njBgRSuZsOgFPrHLzTkeT_XYjqI06J48MtI9q-inpQ3Ag";

        tokenResponse.put("id_token", idToken);
        final IdTokenDto expected = idTokenDtoBuilder.build();
        final IdTokenDto result = oneLoginService.decodeTokenId(idToken);
        Assertions.assertEquals(expected, result);
    }

    @Nested
    class IsNonceExpiredTest {

        @Test
        void testNonceNotExpired() {
            final Nonce.NonceBuilder nonceBuilder = Nonce.builder()
                    .nonceId(1)
                    .nonceString("nonce")
                    .createdAt(new Date());
            final Nonce nonceObj = nonceBuilder.build();
            assertFalse(oneLoginService.isNonceExpired(nonceObj));
        }

        @Test
        void testNonceIsExpired() throws ParseException {
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            final Nonce.NonceBuilder nonceBuilder = Nonce.builder()
                    .nonceId(1)
                    .nonceString("nonce")
                    .createdAt(format.parse("1970-01-01"));
            final Nonce nonceObj = nonceBuilder.build();
            assertTrue(oneLoginService.isNonceExpired(nonceObj));
        }

        @Test
        void testNonceDoesNotExist() {
            assertTrue(oneLoginService.isNonceExpired(new Nonce()));
        }
    }

    @Nested
    class ValidateIdTokenTest {

        @Test
        void testValidIdToken() {
            IdTokenDto validToken = new IdTokenDto();
            validToken.setIss(DUMMY_BASE_URL.concat("/"));
            validToken.setAud(DUMMY_CLIENT_ID);
            validToken.setExp(Instant.now().plus(Duration.ofHours(1)).getEpochSecond());
            validToken.setIat(Instant.now().minus(Duration.ofHours(1)).getEpochSecond());

            assertDoesNotThrow(() -> oneLoginService.validateIdToken(validToken));
        }

        @Test
        void testInvalidIss() {
            IdTokenDto invalidToken = new IdTokenDto();
            invalidToken.setIss("invalidBaseUrl");
            invalidToken.setAud(DUMMY_CLIENT_ID);
            invalidToken.setExp(Instant.now().plus(Duration.ofHours(1)).getEpochSecond());
            invalidToken.setIat(Instant.now().minus(Duration.ofHours(1)).getEpochSecond());

            assertThrows(UnauthorizedClientException.class, () -> oneLoginService.validateIdToken(invalidToken));
        }

        @Test
        void testInvalidAud() {
            IdTokenDto invalidToken = new IdTokenDto();
            invalidToken.setIss(DUMMY_BASE_URL);
            invalidToken.setAud("invalidClientId");
            invalidToken.setExp(Instant.now().plus(Duration.ofHours(1)).getEpochSecond());
            invalidToken.setIat(Instant.now().minus(Duration.ofHours(1)).getEpochSecond());

            assertThrows(UnauthorizedClientException.class, () -> oneLoginService.validateIdToken(invalidToken));
        }

        @Test
        void testExpiredToken() {
            IdTokenDto expiredToken = new IdTokenDto();
            expiredToken.setIss(DUMMY_BASE_URL);
            expiredToken.setAud(DUMMY_CLIENT_ID);
            expiredToken.setExp(Instant.now().minus(Duration.ofMinutes(1)).getEpochSecond());
            expiredToken.setIat(Instant.now().minus(Duration.ofHours(1)).getEpochSecond());

            assertThrows(UnauthorizedClientException.class, () -> oneLoginService.validateIdToken(expiredToken));
        }

        @Test
        void testFutureIssuedToken() {
            IdTokenDto futureIssuedToken = new IdTokenDto();
            futureIssuedToken.setIss(DUMMY_BASE_URL);
            futureIssuedToken.setAud(DUMMY_CLIENT_ID);
            futureIssuedToken.setExp(Instant.now().plus(Duration.ofHours(1)).getEpochSecond());
            futureIssuedToken.setIat(Instant.now().plus(Duration.ofHours(1)).getEpochSecond());

            assertThrows(UnauthorizedClientException.class, () -> oneLoginService.validateIdToken(futureIssuedToken));
        }
    }


    @Nested
    class getOneLoginAuthorizeUrl {

        @Test
        void testStateSetCorrectly(){
            String state = "state";
            String nonce = "nonce";
            String actualUrl = oneLoginService.getOneLoginAuthorizeUrl(state, nonce);

            assertThat(actualUrl).contains("&state=" + state);
        }

        @Test
        void testNonceSetCorrectly(){
            String state = "state";
            String nonce = "nonce";
            String actualUrl = oneLoginService.getOneLoginAuthorizeUrl(state, nonce);

            assertThat(actualUrl).contains("&nonce=" + nonce);
        }

        @Test
        void testMfaEnabled(){
            String nonce = "nonce";
            String state = "state";
            String actualUrl = oneLoginService.getOneLoginAuthorizeUrl(state, nonce);

            assertThat(actualUrl).contains("&vtr=[\"Cl.Cm\"]");
        }

        @Test
        void testMfaDisabled(){
            ReflectionTestUtils.setField(oneLoginService, "mfaEnabled", false);
            String nonce = "nonce";
            String state = "state";
            String actualUrl = oneLoginService.getOneLoginAuthorizeUrl(state, nonce);

            assertThat(actualUrl).contains("&vtr=[\"Cl\"]");
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
