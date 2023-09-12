package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetofice.gapuserservice.config.FindAGrantConfigProperties;
import gov.cabinetofice.gapuserservice.dto.IdTokenDto;
import gov.cabinetofice.gapuserservice.dto.OneLoginUserInfoDto;
import gov.cabinetofice.gapuserservice.dto.PrivacyPolicyDto;
import gov.cabinetofice.gapuserservice.dto.StateCookieDto;
import gov.cabinetofice.gapuserservice.enums.LoginJourneyState;
import gov.cabinetofice.gapuserservice.exceptions.UnauthorizedClientException;
import gov.cabinetofice.gapuserservice.exceptions.NonceExpiredException;
import gov.cabinetofice.gapuserservice.model.Nonce;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.repository.NonceRepository;
import gov.cabinetofice.gapuserservice.service.OneLoginService;
import gov.cabinetofice.gapuserservice.service.encryption.Sha512Service;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import gov.cabinetofice.gapuserservice.util.LoggingUtils;
import gov.cabinetofice.gapuserservice.util.WebUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.WebUtils;
import org.json.JSONObject;

import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginControllerV2Test {

    private LoginControllerV2 loginController;

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private OneLoginService oneLoginService;

    @Mock
    private ApplicationConfigProperties configProperties;

    @Mock
    private FindAGrantConfigProperties findProperties;

    @Mock
    private CustomJwtServiceImpl customJwtService;

    @Mock
    private Sha512Service encryptionService;

    @Mock
    private NonceRepository nonceRepository;

    @Mock
    private LoggingUtils loggingUtils;

    private static MockedStatic<WebUtils> mockedWebUtils;

    @BeforeEach
    void setUp() {
        mockedWebUtils = mockStatic(WebUtils.class);

        configProperties = ApplicationConfigProperties.builder()
                .defaultRedirectUrl("https://www.find-government-grants.service.gov.uk/")
                .build();

        loginController = new LoginControllerV2(oneLoginService, customJwtService, configProperties, encryptionService, nonceRepository, findProperties, loggingUtils);
        ReflectionTestUtils.setField(loginController, "userServiceCookieName", "userServiceCookieName");
        ReflectionTestUtils.setField(loginController, "adminBaseUrl", "/adminBaseUrl");
        ReflectionTestUtils.setField(loginController, "applicantBaseUrl", "/applicantBaseUrl");
        ReflectionTestUtils.setField(loginController, "migrationEnabled", "true");
        ReflectionTestUtils.setField(loginController, "techSupportAppBaseUrl", "/techSupportAppBaseUrl");
    }

    @AfterEach
    public void close() {
        mockedWebUtils.close();
    }

    @Nested
    class login {
        final String state = "state";
        final String nonce = "nonce";
        final String saltId = "saltId";
        final String loginUrl = "loginUrl";
        @Test
        void shouldRedirectToLoginPage_IfTokenIsNull_AndMigrationJourneyDisabled() {
            ReflectionTestUtils.setField(loginController, "migrationEnabled", "false");
            final Optional<String> redirectUrl = Optional.of("https://www.find-government-grants.service.gov.uk/");
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final MockHttpServletRequest request = new MockHttpServletRequest();

            when(oneLoginService.buildEncodedStateJson(redirectUrl.get(), state, saltId)).thenCallRealMethod();
            when(oneLoginService.generateState()).thenReturn(state);
            when(oneLoginService.generateAndStoreNonce()).thenReturn(nonce);
            when(oneLoginService.generateAndStoreState(response, redirectUrl.get(), saltId)).thenCallRealMethod();
            when(oneLoginService.getOneLoginAuthorizeUrl(state, nonce)).thenReturn(loginUrl);
            when(encryptionService.getSHA512SecurePassword(any(), eq(saltId))).thenReturn(state);
            when(encryptionService.generateAndStoreSalt()).thenReturn(saltId);

            final RedirectView methodResponse = loginController.login(redirectUrl, request, response);

            ArgumentCaptor<Cookie> argument = ArgumentCaptor.forClass(Cookie.class);
            verify(response).addCookie(argument.capture());
            Cookie stateCookie = argument.getValue();

            assertThat(stateCookie.getName()).isEqualTo(loginController.getSTATE_COOKIE());
            assertThat(stateCookie.getValue()).isEqualTo("eyJyZWRpcmVjdFVybCI6Imh0dHBzOi8vd3d3LmZpbmQtZ292ZXJubWVudC1ncmFudHMuc2VydmljZS5nb3YudWsvIiwic2FsdElkIjoic2FsdElkIiwic3RhdGUiOiJzdGF0ZSJ9");
            assertThat(stateCookie.isHttpOnly()).isEqualTo(true);
            assertThat(stateCookie.getSecure()).isEqualTo(true);
            assertThat(stateCookie.getMaxAge()).isEqualTo(3600);

            assertThat(methodResponse.getUrl()).isEqualTo(loginUrl);
        }

        @Test
        void shouldReturnRedirectUrl_IfOneIsProvided_AndTokenIsValid() {
            final String customToken = "a-custom-valid-token";
            final Optional<String> redirectUrl = Optional.of("https://www.find-government-grants.service.gov.uk/");
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final MockHttpServletRequest request = new MockHttpServletRequest();

            mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                    .thenReturn(new Cookie(LoginController.REDIRECT_URL_COOKIE, customToken));
            when(customJwtService.isTokenValid(customToken))
                    .thenReturn(true);

            final RedirectView methodResponse = loginController.login(redirectUrl, request, response);

            verify(customJwtService, times(0)).generateToken(any());
            assertThat(methodResponse.getUrl()).isEqualTo(redirectUrl.get());
        }

        @Test
        void shouldReturnDefaultRedirectUrl_IfRedirectUrlNotProvided_AndTokenIsValid() {
            final String customToken = "a-custom-valid-token";
            final Optional<String> redirectUrl = Optional.empty();
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final MockHttpServletRequest request = new MockHttpServletRequest();

            mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                    .thenReturn(new Cookie(LoginController.REDIRECT_URL_COOKIE, customToken));
            when(customJwtService.isTokenValid(customToken))
                    .thenReturn(true);
            final RedirectView methodResponse = loginController.login(redirectUrl, request, response);

            verify(customJwtService, times(0)).generateToken(any());
            assertThat(methodResponse.getUrl()).isEqualTo(configProperties.getDefaultRedirectUrl());
        }
    }

    @Nested
    class redirectAfterLogin {
        //B64 encoded stateCookie containing '{"state":"state","redirectUrl":"redirectUrl"}'
        final String stateCookie = "eyJzdGF0ZSI6InN0YXRlIiwicmVkaXJlY3RVcmwiOiJyZWRpcmVjdFVybCJ9";
        final String redirectUrlCookie = "redirectUrl";
        final String code = "code";
        final String state = "state";
        final String nonce = "nonce";
        final String saltId = "saltId";
        final String idToken = "idToken";
        final String accessToken = "accessToken";
        final User.UserBuilder userBuilder = User.builder()
                .emailAddress("email")
                .sub("sub")
                .loginJourneyState(LoginJourneyState.PRIVACY_POLICY_PENDING)
                .roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build()));
        final IdTokenDto.IdTokenDtoBuilder idTokenDtoBuilder = IdTokenDto.builder()
                .nonce(nonce);
        final Nonce.NonceBuilder nonceBuilder = Nonce.builder()
                .nonceId(1)
                .nonceString(nonce)
                .createdAt(new Date());
        final StateCookieDto.StateCookieDtoBuilder stateCookieDtoBuilder = StateCookieDto.builder()
                .state(state)
                .redirectUrl(redirectUrlCookie)
                .saltId(saltId);

        @Test
        void shouldFetchOneLoginUserInfo() throws JSONException {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final JSONObject tokenResponse = new JSONObject();
            tokenResponse.put("id_token", idToken).put("access_token", accessToken);

            final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                    .emailAddress("email")
                    .sub("sub")
                    .build();

            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
            when(oneLoginService.getOneLoginUserInfoDto(accessToken)).thenReturn(oneLoginUserInfoDto);
            when(oneLoginService.createOrGetUserFromInfo(any())).thenReturn(userBuilder.build());
            when(oneLoginService.decodeTokenId(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());
            when(oneLoginService.readAndDeleteNonce(nonce)).thenReturn(nonceBuilder.build());
            when(encryptionService.getSHA512SecurePassword(any(), eq(saltId))).thenReturn(state);

            loginController.redirectAfterLogin(stateCookie, response, code, state);

            verify(oneLoginService).getOneLoginUserInfoDto(accessToken);
        }

        @Test
        void shouldCreateOrGetUserFromOneLoginInfo() throws JSONException {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final JSONObject tokenResponse = new JSONObject();
            tokenResponse.put("id_token", idToken).put("access_token", accessToken);

            final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                    .emailAddress("email")
                    .sub("sub")
                    .build();

            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
            when(oneLoginService.getOneLoginUserInfoDto(accessToken)).thenReturn(oneLoginUserInfoDto);
            when(oneLoginService.createOrGetUserFromInfo(oneLoginUserInfoDto)).thenReturn(userBuilder.build());
            when(oneLoginService.decodeTokenId(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());
            when(oneLoginService.readAndDeleteNonce(nonce)).thenReturn(nonceBuilder.build());
            when(encryptionService.getSHA512SecurePassword(any(), eq(saltId))).thenReturn(state);

            loginController.redirectAfterLogin(stateCookie, response, code, state);

            verify(oneLoginService).createOrGetUserFromInfo(oneLoginUserInfoDto);
        }

        @Test
        void shouldCreateJwtCookie() throws JSONException {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final Map<String, String> claims = Map.of("claim1", "value1", "claim2", "value2");
            final Cookie cookie = WebUtil.buildSecureCookie("userServiceCookieName", "jwtToken");
            final JSONObject tokenResponse = new JSONObject();
            tokenResponse.put("id_token", idToken).put("access_token", accessToken);

            final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                    .emailAddress("email")
                    .sub("sub")
                    .build();

            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
            when(oneLoginService.getOneLoginUserInfoDto(accessToken)).thenReturn(oneLoginUserInfoDto);
            when(oneLoginService.createOrGetUserFromInfo(any())).thenReturn(userBuilder.build());
            when(oneLoginService.decodeTokenId(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());
            when(oneLoginService.generateCustomJwtClaims(any(), any())).thenReturn(claims);
            when(customJwtService.generateToken(claims)).thenReturn("jwtToken");
            when(oneLoginService.readAndDeleteNonce(nonce)).thenReturn(nonceBuilder.build());
            when(encryptionService.getSHA512SecurePassword(any(), eq(saltId))).thenReturn(state);

            loginController.redirectAfterLogin(stateCookie, response, code, state);

            verify(response).addCookie(cookie);
        }

        @Test
        void shouldRedirectToPrivacyPolicyPage_whenItHasNotBeenAccepted() throws JSONException {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final JSONObject tokenResponse = new JSONObject();
            tokenResponse.put("id_token", idToken).put("access_token", accessToken);
            final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                    .emailAddress("email")
                    .sub("sub")
                    .build();

            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
            when(oneLoginService.getOneLoginUserInfoDto(accessToken)).thenReturn(oneLoginUserInfoDto);
            when(oneLoginService.createOrGetUserFromInfo(any())).thenReturn(userBuilder.build());
            when(oneLoginService.decodeTokenId(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());
            when(oneLoginService.readAndDeleteNonce(nonce)).thenReturn(nonceBuilder.build());
            when(encryptionService.getSHA512SecurePassword(any(), eq(saltId))).thenReturn(state);

            final RedirectView methodResponse = loginController.redirectAfterLogin(stateCookie, response, code, state);

            assertThat(methodResponse.getUrl()).isEqualTo("privacy-policy");
        }

        @Test
        void shouldRedirectToSADashboard_whenUserIsSA() throws JSONException {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final User user = userBuilder
                    .roles(List.of(Role.builder().name(RoleEnum.SUPER_ADMIN).build()))
                    .loginJourneyState(LoginJourneyState.USER_READY)
                    .build();
            final JSONObject tokenResponse = new JSONObject();
            tokenResponse.put("id_token", idToken).put("access_token", accessToken);

            final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                    .emailAddress("email")
                    .sub("sub")
                    .build();

            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
            when(oneLoginService.getOneLoginUserInfoDto(accessToken)).thenReturn(oneLoginUserInfoDto);
            when(oneLoginService.createOrGetUserFromInfo(any())).thenReturn(user);
            when(oneLoginService.decodeTokenId(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());
            when(oneLoginService.readAndDeleteNonce(nonce)).thenReturn(nonceBuilder.build());
            when(encryptionService.getSHA512SecurePassword(any(), eq(saltId))).thenReturn(state);

            final RedirectView methodResponse = loginController.redirectAfterLogin(stateCookie, response, code, state);


            assertThat(methodResponse.getUrl()).isEqualTo("/adminBaseUrl?redirectUrl=/super-admin-dashboard");
        }

        @Test
        void shouldRedirectToAdminDashboard_whenUserIsAdmin() throws JSONException {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final User user = userBuilder
                    .roles(List.of(Role.builder().name(RoleEnum.ADMIN).build()))
                    .loginJourneyState(LoginJourneyState.USER_READY)
                    .build();
            final JSONObject tokenResponse = new JSONObject();
            tokenResponse.put("id_token", idToken).put("access_token", accessToken);

            final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                    .emailAddress("email")
                    .sub("sub")
                    .build();

            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
            when(oneLoginService.getOneLoginUserInfoDto(accessToken)).thenReturn(oneLoginUserInfoDto);
            when(oneLoginService.createOrGetUserFromInfo(any())).thenReturn(user);
            when(oneLoginService.decodeTokenId(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());
            when(oneLoginService.readAndDeleteNonce(nonce)).thenReturn(nonceBuilder.build());
            when(encryptionService.getSHA512SecurePassword(any(), eq(saltId))).thenReturn(state);

            final RedirectView methodResponse = loginController.redirectAfterLogin(stateCookie, response, code, state);

            assertThat(methodResponse.getUrl()).isEqualTo("/adminBaseUrl?redirectUrl=/dashboard");
        }

        @Test
        void shouldRedirectToRedirectUrlCookie_whenUserIsApplicant() throws JSONException {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final User user = userBuilder
                    .roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build()))
                    .loginJourneyState(LoginJourneyState.USER_READY)
                    .build();
            final JSONObject tokenResponse = new JSONObject();
            tokenResponse.put("id_token", idToken).put("access_token", accessToken);


            final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                    .emailAddress("email")
                    .sub("sub")
                    .build();

            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
            when(oneLoginService.getOneLoginUserInfoDto(accessToken)).thenReturn(oneLoginUserInfoDto);
            when(oneLoginService.createOrGetUserFromInfo(any())).thenReturn(user);
            when(oneLoginService.decodeTokenId(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());
            when(oneLoginService.readAndDeleteNonce(nonce)).thenReturn(nonceBuilder.build());
            when(encryptionService.getSHA512SecurePassword(any(), eq(saltId))).thenReturn(state);

            final RedirectView methodResponse = loginController.redirectAfterLogin(stateCookie, response, code, state);

            assertThat(methodResponse.getUrl()).isEqualTo(redirectUrlCookie);
        }

        @Test
        void shouldRedirectToTechSupportDashboard_whenUserIsTechSupport() throws JSONException {

            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final User user = userBuilder
                    .roles(List.of(Role.builder().name(RoleEnum.TECHNICAL_SUPPORT).build()))
                    .loginJourneyState(LoginJourneyState.USER_READY)
                    .build();
            final JSONObject tokenResponse = new JSONObject();
            tokenResponse.put("id_token", idToken).put("access_token", accessToken);

            final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                    .emailAddress("email")
                    .sub("sub")
                    .build();

            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
            when(oneLoginService.getOneLoginUserInfoDto(accessToken)).thenReturn(oneLoginUserInfoDto);
            when(oneLoginService.createOrGetUserFromInfo(any())).thenReturn(user);
            when(oneLoginService.decodeTokenId(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());
            when(oneLoginService.readAndDeleteNonce(nonce)).thenReturn(nonceBuilder.build());
            when(encryptionService.getSHA512SecurePassword(any(), eq(saltId))).thenReturn(state);

            final RedirectView methodResponse = loginController.redirectAfterLogin(stateCookie, response, code, state);

            assertThat(methodResponse.getUrl()).isEqualTo("/techSupportAppBaseUrl");
        }

        @Test
        void shouldRequireReauthentication_ifNonceIsExpired() throws JSONException {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final JSONObject tokenResponse = new JSONObject();
            tokenResponse.put("id_token", idToken).put("access_token", accessToken);

            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
            final Nonce.NonceBuilder nonceBuilder = Nonce.builder()
                    .nonceId(1)
                    .nonceString(nonce)
                    .createdAt(null);
            final Nonce nonceObj = nonceBuilder.build();

            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
            when(oneLoginService.decodeTokenId(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());
            when(oneLoginService.readAndDeleteNonce("nonce")).thenReturn(nonceObj);
            when(oneLoginService.isNonceExpired(nonceObj)).thenReturn(true);
            when(encryptionService.getSHA512SecurePassword(any(), eq(saltId))).thenReturn(state);

            Exception exception = assertThrows(NonceExpiredException.class, () -> loginController.redirectAfterLogin(stateCookie, response, code, state));
            assertThat(exception.getMessage()).isEqualTo("User authorization failed, please try again");
        }

        @Test
        void shouldRequireReauthentication_ifNonceDoesNotMatch() throws JSONException {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final JSONObject tokenResponse = new JSONObject();
            tokenResponse.put("id_token", idToken).put("access_token", accessToken);

            final Nonce.NonceBuilder nonceBuilder = Nonce.builder()
                    .nonceId(1)
                    .nonceString("invalid_nonce")
                    .createdAt(new Date());
            final Nonce nonceObj = nonceBuilder.build();

            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
            when(oneLoginService.decodeTokenId(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());
            when(oneLoginService.readAndDeleteNonce("nonce")).thenReturn(nonceObj);
            when(oneLoginService.isNonceExpired(nonceObj)).thenReturn(false);
            when(encryptionService.getSHA512SecurePassword(any(), eq(saltId))).thenReturn(state);

            Exception exception = assertThrows(UnauthorizedClientException.class, () -> loginController.redirectAfterLogin(stateCookie, response, code, state));
            assertThat(exception.getMessage()).isEqualTo("User authorization failed");
        }

        @Test
        void shouldRequireReauthentication_ifNonceDoesNotExist() throws JSONException {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final JSONObject tokenResponse = new JSONObject();
            tokenResponse.put("id_token", idToken).put("access_token", accessToken);
            final Nonce nonExistentNonce = new Nonce();

            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
            when(oneLoginService.decodeTokenId(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());
            when(oneLoginService.readAndDeleteNonce("nonce")).thenReturn(nonExistentNonce);
            when(oneLoginService.isNonceExpired(nonExistentNonce)).thenReturn(true);
            when(encryptionService.getSHA512SecurePassword(any(), eq(saltId))).thenReturn(state);

            Exception exception = assertThrows(UnauthorizedClientException.class, () -> loginController.redirectAfterLogin(stateCookie, response, code, state));
            assertThat(exception.getMessage()).isEqualTo("User authorization failed");
        }

        @Test
        void shouldRequireReauthentication_ifStateDoesNotMatch() throws JSONException {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final JSONObject tokenResponse = new JSONObject();
            tokenResponse.put("id_token", idToken).put("access_token", accessToken);;
            final Nonce.NonceBuilder nonceBuilder = Nonce.builder()
                    .nonceId(1)
                    .nonceString(nonce)
                    .createdAt(new Date());
            final Nonce nonceObj = nonceBuilder.build();

            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
            when(oneLoginService.decodeTokenId(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());
            when(oneLoginService.readAndDeleteNonce("nonce")).thenReturn(nonceObj);
            when(oneLoginService.isNonceExpired(nonceObj)).thenReturn(false);
            when(encryptionService.getSHA512SecurePassword(any(), eq(saltId))).thenReturn("invalid_state");

            Exception exception = assertThrows(UnauthorizedClientException.class, () -> loginController.redirectAfterLogin(stateCookie, response, code, state));
            assertThat(exception.getMessage()).isEqualTo("User authorization failed");
        }
    }

    @Nested
    class privacyPolicy {
        private final String mockJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzdWIiLCJlbWFpbCI6ImVtYWlsIiwicm9sZXMiOlsiRklORCIsIkFQUExJQ0FOVCJdfQ.MrlNeug1Wos6UYKgwSBHxFw0XxdgQvcCdO-Xi3RMqBk";

        @Test
        void whenPrivacyPolicyPageHasNotBeenAccepted_UserIsSentBackToPrivacyPolicyPage() {
            final PrivacyPolicyDto privacyPolicyDto = PrivacyPolicyDto.builder().acceptPrivacyPolicy("no").build();
            final BindingResult result = Mockito.mock(BindingResult.class);
            final MockHttpServletRequest request = new MockHttpServletRequest();
            final String redirectUrl = "redirectUrl";

            mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                    .thenReturn(new Cookie("userServiceCookieName", mockJwt));
            when(result.hasErrors()).thenReturn(true);

            final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto, result, request, Optional.of(redirectUrl));
            assertThat(methodResponse.getViewName()).isEqualTo(LoginControllerV2.PRIVACY_POLICY_PAGE_VIEW);
            verify(oneLoginService,  times(0)).setUsersLoginJourneyState(any(), any());
        }

        @Test
        void whenPrivacyPolicyPageHasBeenAccepted_ApplicantIsSentToRedirectCookie() {
            final PrivacyPolicyDto privacyPolicyDto = PrivacyPolicyDto.builder().acceptPrivacyPolicy("yes").build();
            final BindingResult result = Mockito.mock(BindingResult.class);
            final MockHttpServletRequest request = new MockHttpServletRequest();
            final String redirectUrl = "redirectUrl";
            final User user = User.builder()
                    .sub("sub")
                    .loginJourneyState(LoginJourneyState.PRIVACY_POLICY_PENDING)
                    .roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build()))
                    .build();

            mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                    .thenReturn(new Cookie("userServiceCookieName", mockJwt));
            when(result.hasErrors()).thenReturn(false);
            when(oneLoginService.getUserFromSub(anyString())).thenReturn(Optional.of(user));

            final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto, result,
                    request, Optional.of(redirectUrl));

            assertThat(methodResponse.getViewName()).isEqualTo("redirect:" + redirectUrl);
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.PRIVACY_POLICY_ACCEPTED);
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.USER_READY);
        }

        @Test
        void whenPrivacyPolicyPageHasBeenAccepted_AdminIsSentToAdminDashboard() {
            final PrivacyPolicyDto privacyPolicyDto = PrivacyPolicyDto.builder().acceptPrivacyPolicy("yes").build();
            final BindingResult result = Mockito.mock(BindingResult.class);
            final MockHttpServletRequest request = new MockHttpServletRequest();
            final String redirectUrl = "redirectUrl";
            final User user = User.builder()
                    .sub("sub")
                    .loginJourneyState(LoginJourneyState.PRIVACY_POLICY_PENDING)
                    .roles(List.of(Role.builder().name(RoleEnum.ADMIN).build()))
                    .build();

            mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                    .thenReturn(new Cookie("userServiceCookieName", mockJwt));
            when(result.hasErrors()).thenReturn(false);
            when(oneLoginService.getUserFromSub(anyString())).thenReturn(Optional.of(user));

            final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto, result,
                    request, Optional.of(redirectUrl));

            assertThat(methodResponse.getViewName()).isEqualTo("redirect:/adminBaseUrl?redirectUrl=/dashboard");
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.PRIVACY_POLICY_ACCEPTED);
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.USER_READY);
        }

        @Test
        void whenPrivacyPolicyPageHasBeenAccepted_SuperAdminIsSentToAdminDashboard() {
            final PrivacyPolicyDto privacyPolicyDto = PrivacyPolicyDto.builder().acceptPrivacyPolicy("yes").build();
            final BindingResult result = Mockito.mock(BindingResult.class);
            final MockHttpServletRequest request = new MockHttpServletRequest();
            final String redirectUrl = "redirectUrl";
            final User user = User.builder()
                    .sub("sub")
                    .loginJourneyState(LoginJourneyState.PRIVACY_POLICY_PENDING)
                    .roles(List.of(Role.builder().name(RoleEnum.SUPER_ADMIN).build()))
                    .build();

            mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                    .thenReturn(new Cookie("userServiceCookieName", mockJwt));
            when(result.hasErrors()).thenReturn(false);
            when(oneLoginService.getUserFromSub(anyString())).thenReturn(Optional.of(user));

            final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto, result,
                    request, Optional.of(redirectUrl));

            assertThat(methodResponse.getViewName()).isEqualTo("redirect:/adminBaseUrl?redirectUrl=/super-admin-dashboard");
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.PRIVACY_POLICY_ACCEPTED);
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.USER_READY);
        }

        @Test
        void whenPrivacyPolicyPageHasBeenAccepted_TechnicalSupportUserIsSentToTechnicalSupportDashboard() {
            final PrivacyPolicyDto privacyPolicyDto = PrivacyPolicyDto.builder().acceptPrivacyPolicy("yes").build();
            final BindingResult result = Mockito.mock(BindingResult.class);
            final MockHttpServletRequest request = new MockHttpServletRequest();
            final String redirectUrl = "redirectUrl";
            final User user = User.builder()
                    .sub("sub")
                    .loginJourneyState(LoginJourneyState.PRIVACY_POLICY_PENDING)
                    .roles(List.of(Role.builder().name(RoleEnum.TECHNICAL_SUPPORT).build()))
                    .build();

            mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                    .thenReturn(new Cookie("userServiceCookieName", mockJwt));
            when(result.hasErrors()).thenReturn(false);
            when(oneLoginService.getUserFromSub(anyString())).thenReturn(Optional.of(user));

            final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto, result,
                    request, Optional.of(redirectUrl));

            assertThat(methodResponse.getViewName()).isEqualTo("redirect:/techSupportAppBaseUrl");
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.PRIVACY_POLICY_ACCEPTED);
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.USER_READY);
        }

        @Test
        void shouldMigrateOldColaAdminSuccess() {
            final PrivacyPolicyDto privacyPolicyDto = PrivacyPolicyDto.builder().acceptPrivacyPolicy("yes").build();
            final BindingResult result = Mockito.mock(BindingResult.class);
            final MockHttpServletRequest request = new MockHttpServletRequest();
            final String redirectUrl = "redirectUrl";
            final User user = User.builder()
                    .sub("sub")
                    .colaSub(UUID.randomUUID())
                    .loginJourneyState(LoginJourneyState.PRIVACY_POLICY_PENDING)
                    .roles(List.of(Role.builder().name(RoleEnum.ADMIN).build()))
                    .build();

            mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                    .thenReturn(new Cookie("userServiceCookieName", mockJwt));
            when(result.hasErrors()).thenReturn(false);
            when(oneLoginService.getUserFromSub(anyString())).thenReturn(Optional.of(user));
            doNothing().when(oneLoginService).migrateUser(user, mockJwt);

            final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto,
                    result, request, Optional.of(redirectUrl));

            assertThat(methodResponse.getViewName()).isEqualTo("redirect:/adminBaseUrl?redirectUrl=/dashboard?migrationStatus=success");
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.PRIVACY_POLICY_ACCEPTED);
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.MIGRATING_USER);
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.MIGRATION_SUCCEEDED);
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.USER_READY);
        }

        @Test
        void shouldMigrateOldColaAdminFail() {
            final PrivacyPolicyDto privacyPolicyDto = PrivacyPolicyDto.builder().acceptPrivacyPolicy("yes").build();
            final BindingResult result = Mockito.mock(BindingResult.class);
            final MockHttpServletRequest request = new MockHttpServletRequest();
            final String redirectUrl = "redirectUrl";
            final User user = User.builder()
                    .sub("sub")
                    .colaSub(UUID.randomUUID())
                    .loginJourneyState(LoginJourneyState.PRIVACY_POLICY_PENDING)
                    .roles(List.of(Role.builder().name(RoleEnum.ADMIN).build()))
                    .build();

            mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                    .thenReturn(new Cookie("userServiceCookieName", mockJwt));
            when(result.hasErrors()).thenReturn(false);
            when(oneLoginService.getUserFromSub(anyString())).thenReturn(Optional.of(user));
            doThrow(new RuntimeException()).when(oneLoginService).migrateUser(user, mockJwt);

            final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto,
                    result, request, Optional.of(redirectUrl));

            assertThat(methodResponse.getViewName()).isEqualTo("redirect:/adminBaseUrl?redirectUrl=/dashboard?migrationStatus=error");
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.PRIVACY_POLICY_ACCEPTED);
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.MIGRATING_USER);
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.MIGRATION_FAILED);
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.USER_READY);
        }

        @Test
        void shouldMigrateOldColaApplicantSuccess() {
            final PrivacyPolicyDto privacyPolicyDto = PrivacyPolicyDto.builder().acceptPrivacyPolicy("yes").build();
            final BindingResult result = Mockito.mock(BindingResult.class);
            final MockHttpServletRequest request = new MockHttpServletRequest();
            final String redirectUrl = "/redirectUrl";
            final User user = User.builder()
                    .sub("sub")
                    .colaSub(UUID.randomUUID())
                    .loginJourneyState(LoginJourneyState.PRIVACY_POLICY_PENDING)
                    .roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build()))
                    .build();

            mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                    .thenReturn(new Cookie("userServiceCookieName", mockJwt));
            when(result.hasErrors()).thenReturn(false);
            when(oneLoginService.getUserFromSub(anyString())).thenReturn(Optional.of(user));
            doNothing().when(oneLoginService).migrateUser(user, mockJwt);

            final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto, result,
                    request, Optional.of(redirectUrl));

            assertThat(methodResponse.getViewName()).isEqualTo("redirect:/redirectUrl?migrationStatus=success");
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.PRIVACY_POLICY_ACCEPTED);
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.MIGRATING_USER);
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.MIGRATION_SUCCEEDED);
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.USER_READY);
        }

        @Test
        void shouldMigrateOldColaApplicantFail() {
            final PrivacyPolicyDto privacyPolicyDto = PrivacyPolicyDto.builder().acceptPrivacyPolicy("yes").build();
            final BindingResult result = Mockito.mock(BindingResult.class);
            final MockHttpServletRequest request = new MockHttpServletRequest();
            final String redirectUrl = "/redirectUrl";
            final User user = User.builder()
                    .sub("sub")
                    .colaSub(UUID.randomUUID())
                    .loginJourneyState(LoginJourneyState.PRIVACY_POLICY_PENDING)
                    .roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build()))
                    .build();

            mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                    .thenReturn(new Cookie("userServiceCookieName", mockJwt));
            when(result.hasErrors()).thenReturn(false);
            when(oneLoginService.getUserFromSub(anyString())).thenReturn(Optional.of(user));
            doThrow(new RuntimeException()).when(oneLoginService).migrateUser(user, mockJwt);

            final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto,
                    result, request, Optional.of(redirectUrl));

            assertThat(methodResponse.getViewName()).isEqualTo("redirect:/applicantBaseUrl/api/isNewApplicant?migrationStatus=error");
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.PRIVACY_POLICY_ACCEPTED);
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.MIGRATING_USER);
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.MIGRATION_FAILED);
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.USER_READY);
        }

        @Nested
        class logout {
            @Test
            void testLogoutWithBlankCookie() {
                String userServiceCookieName = "customJWT";
                String applicantBaseUrl = "/applicantBaseUrl";

                HttpServletRequest request = mock(HttpServletRequest.class);
                HttpServletResponse response = mock(HttpServletResponse.class);
                mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName")).thenReturn( new Cookie(userServiceCookieName, "") );
                RedirectView methodResponse = loginController.logout(request, response);

                verify(oneLoginService, never()).logoutUser(any(Cookie.class), any(HttpServletResponse.class));
                Assertions.assertEquals(applicantBaseUrl, methodResponse.getUrl());
            }

            @Test
            void testLogoutWithNonBlankCookie() {
                String userServiceCookieName = "customJWT";
                String applicantBaseUrl = "/applicantBaseUrl";

                HttpServletRequest request = mock(HttpServletRequest.class);
                HttpServletResponse response = mock(HttpServletResponse.class);
                mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName")).thenReturn( new Cookie(userServiceCookieName, "ba") );
                when(oneLoginService.logoutUser(any(), any())).thenReturn(new RedirectView(applicantBaseUrl));

                RedirectView methodResponse = loginController.logout(request, response);

                verify(oneLoginService, times(1)).logoutUser(any(Cookie.class), any(HttpServletResponse.class));
                Assertions.assertEquals(applicantBaseUrl, methodResponse.getUrl());
            }
        }
    }
}
