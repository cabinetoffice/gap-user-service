package gov.cabinetoffice.gapuserservice.web;

import com.auth0.jwt.interfaces.DecodedJWT;
import gov.cabinetoffice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetoffice.gapuserservice.config.FindAGrantConfigProperties;
import gov.cabinetoffice.gapuserservice.dto.*;
import gov.cabinetoffice.gapuserservice.enums.LoginJourneyState;
import gov.cabinetoffice.gapuserservice.enums.MigrationStatus;
import gov.cabinetoffice.gapuserservice.exceptions.NonceExpiredException;
import gov.cabinetoffice.gapuserservice.exceptions.UnauthorizedClientException;
import gov.cabinetoffice.gapuserservice.exceptions.UnauthorizedException;
import gov.cabinetoffice.gapuserservice.exceptions.UserNotFoundException;
import gov.cabinetoffice.gapuserservice.model.Role;
import gov.cabinetoffice.gapuserservice.model.RoleEnum;
import gov.cabinetoffice.gapuserservice.model.User;
import gov.cabinetoffice.gapuserservice.service.OneLoginService;
import gov.cabinetoffice.gapuserservice.service.encryption.Sha512Service;
import gov.cabinetoffice.gapuserservice.service.jwt.TestDecodedJwt;
import gov.cabinetoffice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import gov.cabinetoffice.gapuserservice.service.user.OneLoginUserService;
import gov.cabinetoffice.gapuserservice.util.LoggingUtils;
import gov.cabinetoffice.gapuserservice.util.WebUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.WebUtils;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginControllerV2Test {

    private LoginControllerV2 loginController;

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
    private OneLoginUserService oneLoginUserService;

    @Mock
    private LoggingUtils loggingUtils;
    private static MockedStatic<WebUtils> mockedWebUtils;

    @BeforeEach
    void setUp() {
        mockedWebUtils = mockStatic(WebUtils.class);

        configProperties = ApplicationConfigProperties.builder()
                .defaultRedirectUrl("https://www.find-government-grants.service.gov.uk/")
                .build();

        loginController = new LoginControllerV2(oneLoginService, customJwtService, configProperties, encryptionService, oneLoginUserService, findProperties, loggingUtils);
        ReflectionTestUtils.setField(loginController, "userServiceCookieName", "userServiceCookieName");
        ReflectionTestUtils.setField(loginController, "userServiceCookieDomain", "userServiceCookieDomain");
        ReflectionTestUtils.setField(loginController, "adminBaseUrl", "http:localhost:3000/adminBaseUrl");
        ReflectionTestUtils.setField(loginController, "applicantBaseUrl", "http:localhost:3000/applicantBaseUrl");
        ReflectionTestUtils.setField(loginController, "techSupportAppBaseUrl", "http:localhost:3000/techSupportAppBaseUrl");
        ReflectionTestUtils.setField(loginController, "postLogoutRedirectUri", "http:localhost:3002/postLogoutRedirectUri");
        ReflectionTestUtils.setField(loginController, "findAGrantBaseUrl", "https://www.find-government-grants.service.gov.uk/");
    }

    @AfterEach
    public void close() {
        mockedWebUtils.close();
    }

    @Nested
    class LoginTest {
        final String state = "state";
        final String nonce = "nonce";
        final String saltId = "saltId";
        final String loginUrl = "loginUrl";

        @Test
        void shouldRedirectToLoginPage_IfTokenIsNull_AndMigrationJourneyDisabled() throws MalformedURLException {
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

            assertThat(stateCookie.getName()).isEqualTo("state");
            assertThat(stateCookie.getValue()).isEqualTo("eyJyZWRpcmVjdFVybCI6Imh0dHBzOi8vd3d3LmZpbmQtZ292ZXJubWVudC1ncmFudHMuc2VydmljZS5nb3YudWsvIiwic2FsdElkIjoic2FsdElkIiwic3RhdGUiOiJzdGF0ZSJ9");
            assertThat(stateCookie.isHttpOnly()).isTrue();
            assertThat(stateCookie.getSecure()).isTrue();
            assertThat(stateCookie.getMaxAge()).isEqualTo(3600);

            assertThat(methodResponse.getUrl()).isEqualTo(loginUrl);
        }

        @Test
        void shouldReturnRedirectUrl_IfOneIsProvided_AndTokenIsValid() throws MalformedURLException {
            final String customToken = "a-custom-valid-token";
            final Optional<String> redirectUrl = Optional.of("https://www.find-government-grants.service.gov.uk/");
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final MockHttpServletRequest request = new MockHttpServletRequest();

            mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                    .thenReturn(new Cookie(LoginController.REDIRECT_URL_COOKIE, customToken));
            when(customJwtService.isTokenValid(customToken))
                    .thenReturn(true);

            final RedirectView methodResponse = loginController.login(redirectUrl, request, response);

            verify(customJwtService, times(0)).generateToken(any(), eq(false));
            assertThat(methodResponse.getUrl()).isEqualTo(redirectUrl.get());
        }

        @Test
        void shouldReturnDefaultRedirectUrl_IfRedirectUrlNotProvided_AndTokenIsValid() throws MalformedURLException {
            final String customToken = "a-custom-valid-token";
            final Optional<String> redirectUrl = Optional.empty();
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final MockHttpServletRequest request = new MockHttpServletRequest();

            mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                    .thenReturn(new Cookie(LoginController.REDIRECT_URL_COOKIE, customToken));
            when(customJwtService.isTokenValid(customToken))
                    .thenReturn(true);
            final RedirectView methodResponse = loginController.login(redirectUrl, request, response);

            verify(customJwtService, times(0)).generateToken(any(), eq(false));
            assertThat(methodResponse.getUrl()).isEqualTo(configProperties.getDefaultRedirectUrl());
        }


        @ParameterizedTest
        @CsvSource({"ADMIN, http:localhost:3000/adminBaseUrl/404,true",
                "APPLICANT, http:localhost:3000/applicantBaseUrl/404,false",
                "TECHNICAL_SUPPORT, http:localhost:3000/techSupportAppBaseUrl/404,false",
                "SUPER_ADMIN, http:localhost:3000/adminBaseUrl/404,true",
                "OTHER, /404,false"})
        void shouldReturnToTheCorrectSubdomain404PageWhenUserAsValidTokenButRedirectUrlEndsWith404(String role, String expectedUrl, boolean isAdmin) throws MalformedURLException {
            final String customToken = "a-custom-valid-token";
            final Optional<String> redirectUrl = Optional.of("https://www.find-government-grants.service.gov.uk/404");
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final Cookie cookie = new Cookie(LoginController.REDIRECT_URL_COOKIE, customToken);
            final MockHttpServletRequest request = new MockHttpServletRequest();
            final DecodedJWT decodedJwt = mock(DecodedJWT.class);
            final JwtPayload payload = mock(JwtPayload.class);

            mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                    .thenReturn(cookie);
            when(customJwtService.isTokenValid(customToken))
                    .thenReturn(true);

            when(customJwtService.decodedJwt(any())).thenReturn(decodedJwt);
            when(customJwtService.decodeTheTokenPayloadInAReadableFormat(decodedJwt)).thenReturn(payload);
            when(payload.getRoles()).thenReturn("[" + role + "]");

            final RedirectView methodResponse = loginController.login(redirectUrl, request, response);

            verify(customJwtService, times(0)).generateToken(any(), eq(isAdmin));
            assertThat(methodResponse.getUrl()).isEqualTo(expectedUrl);
        }
    }

    @Nested
    class RedirectAfterLoginTest {
        //B64 encoded stateCookie containing '{"state":"state","redirectUrl":"redirectUrl"}'
        final String stateCookie = "eyJzdGF0ZSI6InN0YXRlIiwicmVkaXJlY3RVcmwiOiJyZWRpcmVjdFVybCJ9";
        final String mockJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzdWIiLCJlbWFpbCI6ImVtYWlsIiwicm9sZXMiOlsiRklORCIsIkFQUExJQ0FOVCJdfQ.MrlNeug1Wos6UYKgwSBHxFw0XxdgQvcCdO-Xi3RMqBk";
        final String redirectUrlCookie = "http://redirectUrl.com";
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
        final StateCookieDto.StateCookieDtoBuilder stateCookieDtoBuilder = StateCookieDto.builder()
                .state(state)
                .redirectUrl(redirectUrlCookie)
                .saltId(saltId);
        final MockHttpServletRequest request = new MockHttpServletRequest();

        @Test
        void shouldFetchOneLoginUserInfo() throws JSONException {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final MockHttpServletRequest request = new MockHttpServletRequest();

            final JSONObject tokenResponse = new JSONObject();
            tokenResponse.put("id_token", idToken).put("access_token", accessToken);

            final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                    .emailAddress("email")
                    .sub("sub")
                    .build();

            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
            when(oneLoginService.getOneLoginUserInfoDto(accessToken)).thenReturn(oneLoginUserInfoDto);
            when(oneLoginUserService.createOrGetUserFromInfo(any())).thenReturn(userBuilder.build());
            when(oneLoginService.decodeTokenId(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());

            loginController.redirectAfterLogin(stateCookie, request, response, code, state);

            verify(oneLoginService).getOneLoginUserInfoDto(accessToken);
        }

        @Test
        void shouldCreateOrGetUserFromOneLoginInfo() throws JSONException {
            final String customToken = "a-custom-valid-token";
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final JSONObject tokenResponse = new JSONObject();
            tokenResponse.put("id_token", idToken).put("access_token", accessToken);

            final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                    .emailAddress("email")
                    .sub("sub")
                    .build();

            final MockHttpServletRequest request = new MockHttpServletRequest();

            mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                    .thenReturn(new Cookie(LoginController.REDIRECT_URL_COOKIE, customToken));
            when(customJwtService.isTokenValid(customToken))
                    .thenReturn(false);

            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
            when(oneLoginService.getOneLoginUserInfoDto(accessToken)).thenReturn(oneLoginUserInfoDto);
            when(oneLoginUserService.createOrGetUserFromInfo(oneLoginUserInfoDto)).thenReturn(userBuilder.build());
            when(oneLoginService.decodeTokenId(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());

            loginController.redirectAfterLogin(stateCookie, request, response, code, state);

            verify(oneLoginUserService).createOrGetUserFromInfo(oneLoginUserInfoDto);
        }

        @Test
        void shouldCreateJwtCookie() throws JSONException {
            final String customToken = "a-custom-valid-token";
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final Map<String, String> claims = Map.of("claim1", "value1", "claim2", "value2");
            final Cookie cookie = WebUtil.buildSecureCookie("userServiceCookieName", "jwtToken", "userServiceCookieDomain");
            final JSONObject tokenResponse = new JSONObject();
            tokenResponse.put("id_token", idToken).put("access_token", accessToken);

            final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                    .emailAddress("email")
                    .sub("sub")
                    .build();

            final MockHttpServletRequest request = new MockHttpServletRequest();

            mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                    .thenReturn(new Cookie(LoginController.REDIRECT_URL_COOKIE, customToken));
            when(customJwtService.isTokenValid(customToken))
                    .thenReturn(false);

            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
            when(oneLoginService.getOneLoginUserInfoDto(accessToken)).thenReturn(oneLoginUserInfoDto);
            when(oneLoginUserService.createOrGetUserFromInfo(any())).thenReturn(userBuilder.build());
            when(oneLoginService.decodeTokenId(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());
            when(oneLoginService.generateCustomJwtClaims(any(), any())).thenReturn(claims);
            when(customJwtService.generateToken(claims, false)).thenReturn("jwtToken");

            loginController.redirectAfterLogin(stateCookie, request, response, code, state);

            verify(response).addCookie(cookie);
        }

        @Test
        void shouldRequireReauthentication_ifNonceIsExpired() throws JSONException {
            final String customToken = "a-custom-valid-token";
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final JSONObject tokenResponse = new JSONObject();
            tokenResponse.put("id_token", idToken).put("access_token", accessToken);

            final MockHttpServletRequest request = new MockHttpServletRequest();

            mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                    .thenReturn(new Cookie(LoginController.REDIRECT_URL_COOKIE, customToken));
            when(customJwtService.isTokenValid(customToken))
                    .thenReturn(false);

            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
            when(oneLoginService.decodeTokenId(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());
            doThrow(new NonceExpiredException("User authorization failed, please try again")).when(oneLoginService).verifyStateAndNonce(any(), any(), any());

            Exception exception = assertThrows(NonceExpiredException.class, () -> loginController.redirectAfterLogin(stateCookie, request, response, code, state));
            assertThat(exception.getMessage()).isEqualTo("User authorization failed, please try again");
        }

        @Test
        void shouldRequireReauthentication_ifStateOrNonceInvalid() throws JSONException {
            final String customToken = "a-custom-valid-token";
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final JSONObject tokenResponse = new JSONObject();
            tokenResponse.put("id_token", idToken).put("access_token", accessToken);
            final MockHttpServletRequest request = new MockHttpServletRequest();

            mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                    .thenReturn(new Cookie(LoginController.REDIRECT_URL_COOKIE, customToken));
            when(customJwtService.isTokenValid(customToken))
                    .thenReturn(false);

            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
            when(oneLoginService.decodeTokenId(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());
            doThrow(new UnauthorizedClientException("User authorization failed")).when(oneLoginService).verifyStateAndNonce(any(), any(), any());

            Exception exception = assertThrows(
                    UnauthorizedClientException.class,
                    () -> loginController.redirectAfterLogin(stateCookie, request, response, code, state)
            );
            assertThat(exception.getMessage()).isEqualTo("User authorization failed");
        }

        @Nested
        class UserReady_AndUserMigratedAndReady {
            @ParameterizedTest
            @CsvSource({"USER_READY,true,false", "USER_READY,true,true",
                    "USER_READY,false,false", "USER_READY,false,true",
                    "USER_MIGRATED_AND_READY,true,false", "USER_MIGRATED_AND_READY,true,true",
                    "USER_MIGRATED_AND_READY,false,false", "USER_MIGRATED_AND_READY,false,true"})
            void shouldRedirectToSADashboard_whenUserIsSA(final LoginJourneyState initialState, final String migrateFindEnabled, final boolean hasRedirectionCookie) {
                final String customToken = "a-custom-valid-token";
                final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
                final User user = userBuilder
                        .roles(List.of(Role.builder().name(RoleEnum.SUPER_ADMIN).build()))
                        .loginJourneyState(initialState)
                        .build();
                final JSONObject tokenResponse = new JSONObject();
                tokenResponse.put("id_token", idToken).put("access_token", accessToken);

                final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                        .emailAddress("email")
                        .sub("sub")
                        .build();

                final MockHttpServletRequest request = new MockHttpServletRequest();

                final StateCookieDto.StateCookieDtoBuilder stateCookieDtoBuilder = StateCookieDto.builder()
                        .state(state)
                        .redirectUrl(hasRedirectionCookie ? "http:localhost:3000/adminBaseUrl/scheme/2/1231231311" : null)
                        .saltId(saltId);
                final Cookie redirectCookie = hasRedirectionCookie ? new Cookie(LoginController.REDIRECT_URL_COOKIE, customToken) : null;
                mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                        .thenReturn(redirectCookie);
                if(hasRedirectionCookie) {
                    when(customJwtService.isTokenValid(customToken))
                            .thenReturn(false);
                }

                ReflectionTestUtils.setField(loginController, "findAccountsMigrationEnabled", migrateFindEnabled);
                when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
                when(oneLoginService.getOneLoginUserInfoDto(accessToken)).thenReturn(oneLoginUserInfoDto);
                when(oneLoginUserService.createOrGetUserFromInfo(any())).thenReturn(user);
                when(oneLoginService.decodeTokenId(any())).thenReturn(idTokenDtoBuilder.build());
                when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());

                final RedirectView methodResponse = loginController.redirectAfterLogin(stateCookie, request, response, code, state);

                if (hasRedirectionCookie&& !initialState.equals(LoginJourneyState.USER_READY)) {
                    assertThat(methodResponse.getUrl()).isEqualTo("http:localhost:3000/adminBaseUrl?redirectUrl=/scheme/2/1231231311");
                } else {
                    assertThat(methodResponse.getUrl()).isEqualTo("http:localhost:3000/adminBaseUrl?redirectUrl=/super-admin-dashboard");
                }
            }

            @ParameterizedTest
            @CsvSource({"USER_READY,true,false", "USER_READY,true,true",
                    "USER_READY,false,false", "USER_READY,false,true",
                    "USER_MIGRATED_AND_READY,true,false", "USER_MIGRATED_AND_READY,true,true",
                    "USER_MIGRATED_AND_READY,false,false", "USER_MIGRATED_AND_READY,false,true"})
            void shouldRedirectToAdminDashboard_whenUserIsAdmin(final LoginJourneyState initialState, final String migrateFindEnabled, final boolean hasRedirectionCookie) {
                final String customToken = "a-custom-valid-token";
                final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
                final User user = userBuilder
                        .roles(List.of(Role.builder().name(RoleEnum.ADMIN).build()))
                        .loginJourneyState(initialState)
                        .applyAccountMigrated(MigrationStatus.ALREADY_MIGRATED)
                        .findAccountMigrated(MigrationStatus.NOT_STARTED)
                        .build();
                final JSONObject tokenResponse = new JSONObject();
                tokenResponse.put("id_token", idToken).put("access_token", accessToken);

                final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                        .emailAddress("email")
                        .sub("sub")
                        .build();

                final MockHttpServletRequest request = new MockHttpServletRequest();

                final StateCookieDto.StateCookieDtoBuilder stateCookieDtoBuilder = StateCookieDto.builder()
                        .state(state)
                        .redirectUrl(hasRedirectionCookie ? "http:localhost:3000/adminBaseUrl/scheme/2/1231231311" : null)
                        .saltId(saltId);
                final Cookie redirectCookie = hasRedirectionCookie ? new Cookie(LoginController.REDIRECT_URL_COOKIE, customToken) : null;
                mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                        .thenReturn(redirectCookie);
                if(hasRedirectionCookie) {
                    when(customJwtService.isTokenValid(customToken))
                            .thenReturn(false);
                }

                ReflectionTestUtils.setField(loginController, "findAccountsMigrationEnabled", migrateFindEnabled);
                when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
                when(oneLoginService.getOneLoginUserInfoDto(accessToken)).thenReturn(oneLoginUserInfoDto);
                when(oneLoginUserService.createOrGetUserFromInfo(any())).thenReturn(user);
                when(oneLoginService.decodeTokenId(any())).thenReturn(idTokenDtoBuilder.build());
                when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());

                final RedirectView methodResponse = loginController.redirectAfterLogin(stateCookie, request, response, code, state);

                if (migrateFindEnabled.equals("true") && initialState.equals(LoginJourneyState.USER_READY)) {
                    assertThat(methodResponse.getUrl()).isEqualTo("http:localhost:3000/adminBaseUrl?redirectUrl=%2Fdashboard%3FapplyMigrationStatus%3DALREADY_MIGRATED%26findMigrationStatus%3DNOT_STARTED");
                }else if  (hasRedirectionCookie && !initialState.equals(LoginJourneyState.USER_READY)) {
                    assertThat(methodResponse.getUrl()).isEqualTo("http:localhost:3000/adminBaseUrl?redirectUrl=/scheme/2/1231231311");
                } else {
                    assertThat(methodResponse.getUrl()).isEqualTo("http:localhost:3000/adminBaseUrl?redirectUrl=/dashboard");
                }
            }

            @ParameterizedTest
            @CsvSource({"USER_READY,true", "USER_READY,false", "USER_MIGRATED_AND_READY,true", "USER_MIGRATED_AND_READY,false"})
            void shouldRedirectToRedirectUrlCookie_whenUserIsApplicant(final LoginJourneyState initialState, final String migrateFindEnabled) {
                final String customToken = "a-custom-valid-token";
                final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
                final User user = userBuilder
                        .roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build()))
                        .loginJourneyState(initialState)
                        .applyAccountMigrated(MigrationStatus.ALREADY_MIGRATED)
                        .findAccountMigrated(MigrationStatus.NOT_STARTED)
                        .build();
                final JSONObject tokenResponse = new JSONObject();
                tokenResponse.put("id_token", idToken).put("access_token", accessToken);
                final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                        .emailAddress("email")
                        .sub("sub")
                        .build();
                final MockHttpServletRequest request = new MockHttpServletRequest();

                mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                        .thenReturn(new Cookie(LoginController.REDIRECT_URL_COOKIE, customToken));
                when(customJwtService.isTokenValid(customToken))
                        .thenReturn(false);

                ReflectionTestUtils.setField(loginController, "findAccountsMigrationEnabled", migrateFindEnabled);
                when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
                when(oneLoginService.getOneLoginUserInfoDto(accessToken)).thenReturn(oneLoginUserInfoDto);
                when(oneLoginUserService.createOrGetUserFromInfo(any())).thenReturn(user);
                when(oneLoginService.decodeTokenId(any())).thenReturn(idTokenDtoBuilder.build());
                when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());

                final RedirectView methodResponse = loginController.redirectAfterLogin(stateCookie, request, response, code, state);

                if (migrateFindEnabled.equals("true") && initialState.equals(LoginJourneyState.USER_READY)) {
                    assertThat(methodResponse.getUrl()).isEqualTo(redirectUrlCookie + "?applyMigrationStatus=ALREADY_MIGRATED&findMigrationStatus=NOT_STARTED");
                } else {
                    assertThat(methodResponse.getUrl()).isEqualTo(redirectUrlCookie);
                }
            }

            @ParameterizedTest
            @CsvSource({"USER_READY,true", "USER_READY,false", "USER_MIGRATED_AND_READY,true", "USER_MIGRATED_AND_READY,false"})
            void shouldRedirectToTechSupportDashboard_whenUserIsTechSupport(final LoginJourneyState initialState, final String migrateFindEnabled) {
                final String customToken = "a-custom-valid-token";
                final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
                final User user = userBuilder
                        .roles(List.of(Role.builder().name(RoleEnum.TECHNICAL_SUPPORT).build()))
                        .loginJourneyState(initialState)
                        .build();
                final JSONObject tokenResponse = new JSONObject();
                tokenResponse.put("id_token", idToken).put("access_token", accessToken);

                final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                        .emailAddress("email")
                        .sub("sub")
                        .build();
                final MockHttpServletRequest request = new MockHttpServletRequest();

                mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                        .thenReturn(new Cookie(LoginController.REDIRECT_URL_COOKIE, customToken));
                when(customJwtService.isTokenValid(customToken))
                        .thenReturn(false);

                ReflectionTestUtils.setField(loginController, "findAccountsMigrationEnabled", migrateFindEnabled);
                when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
                when(oneLoginService.getOneLoginUserInfoDto(accessToken)).thenReturn(oneLoginUserInfoDto);
                when(oneLoginUserService.createOrGetUserFromInfo(any())).thenReturn(user);
                when(oneLoginService.decodeTokenId(any())).thenReturn(idTokenDtoBuilder.build());
                when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());

                final RedirectView methodResponse = loginController.redirectAfterLogin(stateCookie, request, response, code, state);

                assertThat(methodResponse.getUrl()).isEqualTo("http:localhost:3000/techSupportAppBaseUrl");
            }
        }

        @Nested
        class PrivacyPolicyPending {
            @ParameterizedTest
            @ValueSource(strings = {"true", "false"})
            void shouldRedirectToPrivacyPolicyPage_whenItHasNotBeenAccepted(final String migrateFindEnabled) throws JSONException {
                final String customToken = "a-custom-valid-token";
                final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
                final JSONObject tokenResponse = new JSONObject();
                tokenResponse.put("id_token", idToken).put("access_token", accessToken);
                final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                        .emailAddress("email")
                        .sub("sub")
                        .build();
                final MockHttpServletRequest request = new MockHttpServletRequest();

                mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                        .thenReturn(new Cookie(LoginController.REDIRECT_URL_COOKIE, customToken));
                when(customJwtService.isTokenValid(customToken))
                        .thenReturn(false);

                ReflectionTestUtils.setField(loginController, "findAccountsMigrationEnabled", migrateFindEnabled);
                when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
                when(oneLoginService.getOneLoginUserInfoDto(accessToken)).thenReturn(oneLoginUserInfoDto);
                when(oneLoginUserService.createOrGetUserFromInfo(any())).thenReturn(userBuilder.build());
                when(oneLoginService.decodeTokenId(any())).thenReturn(idTokenDtoBuilder.build());
                when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());

                final RedirectView methodResponse = loginController.redirectAfterLogin(stateCookie, request, response, code, state);

                assertThat(methodResponse.getUrl()).isEqualTo("privacy-policy");
            }

            @ParameterizedTest
            @ValueSource(strings = {"true", "false"})
            void whenPrivacyPolicyPageHasNotBeenAccepted_UserIsSentBackToPrivacyPolicyPage(final String migrateFindEnabled) {
                final PrivacyPolicyDto privacyPolicyDto = PrivacyPolicyDto.builder().acceptPrivacyPolicy("no").build();
                final BindingResult result = Mockito.mock(BindingResult.class);
                final MockHttpServletRequest request = new MockHttpServletRequest();
                final String redirectUrl = "http://redirectUrl.com";

                ReflectionTestUtils.setField(loginController, "findAccountsMigrationEnabled", migrateFindEnabled);
                mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                        .thenReturn(new Cookie("userServiceCookieName", "jwt"));
                when(result.hasErrors()).thenReturn(true);


                final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto, result, request, Optional.of(redirectUrl));
                assertThat(methodResponse.getViewName()).isEqualTo(LoginControllerV2.PRIVACY_POLICY_PAGE_VIEW);
            }

            @ParameterizedTest
            @CsvSource({
                    "NOT_STARTED,true,true", "NOT_STARTED,false,true", "NOT_STARTED,true,false", "NOT_STARTED,false,false",
                    "SUCCEEDED,true,true", "SUCCEEDED,false,true", "SUCCEEDED,true,false", "SUCCEEDED,false,false",
                    "FAILED,true,true", "FAILED,false,true", "FAILED,true,false", "FAILED,false,false",
                    "NEW_USER,true,true", "NEW_USER,false,true", "NEW_USER,true,false", "NEW_USER,false,false",
                    "ALREADY_MIGRATED,true,true", "ALREADY_MIGRATED,false,true", "ALREADY_MIGRATED,true,false", "ALREADY_MIGRATED,false,false",
            })
            void whenPrivacyPolicyAccepted_MigrateApplicantUser(final MigrationStatus migrationStatus, final String migrateFindEnabled, final boolean hasColaSub) {
                final PrivacyPolicyDto privacyPolicyDto = PrivacyPolicyDto.builder().acceptPrivacyPolicy("yes").build();
                final BindingResult result = Mockito.mock(BindingResult.class);
                final MockHttpServletRequest request = new MockHttpServletRequest();
                final String redirectUrl = "http://redirectUrl.com";
                final User user = userBuilder
                        .roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build()))
                        .loginJourneyState(LoginJourneyState.PRIVACY_POLICY_PENDING)
                        .colaSub(hasColaSub ? UUID.randomUUID() : null)
                        .applyAccountMigrated(migrationStatus)
                        .findAccountMigrated(migrationStatus)
                        .build();

                ReflectionTestUtils.setField(loginController, "findAccountsMigrationEnabled", migrateFindEnabled);
                mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                        .thenReturn(new Cookie("userServiceCookieName", mockJwt));
                when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());
                when(result.hasErrors()).thenReturn(false);
                when(oneLoginUserService.getUserFromSub(anyString())).thenReturn(Optional.of(user));

                final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto, result, request, Optional.of(redirectUrl));

                assertThat(methodResponse.getViewName()).isEqualTo("redirect:" + redirectUrl + "?applyMigrationStatus=" + migrationStatus + "&findMigrationStatus=" + migrationStatus);
                if (hasColaSub) {
                    verify(oneLoginUserService, times(1)).migrateApplyUser(user, mockJwt);
                } else {
                    verify(oneLoginUserService, times(0)).migrateApplyUser(user, mockJwt);
                }
                if (migrateFindEnabled.equals("true")) {
                    verify(oneLoginUserService, times(1)).migrateFindUser(user, mockJwt);
                    verify(oneLoginUserService, times(1)).setUsersLoginJourneyState(user, LoginJourneyState.USER_MIGRATED_AND_READY);
                    verify(oneLoginUserService, times(0)).setUsersLoginJourneyState(user, LoginJourneyState.USER_READY);
                } else {
                    verify(oneLoginUserService, times(0)).migrateFindUser(user, mockJwt);
                    verify(oneLoginUserService, times(0)).setUsersLoginJourneyState(user, LoginJourneyState.USER_MIGRATED_AND_READY);
                    verify(oneLoginUserService, times(1)).setUsersLoginJourneyState(user, LoginJourneyState.USER_READY);
                }
            }

            @ParameterizedTest
            @CsvSource({
                    "NOT_STARTED,true,true", "NOT_STARTED,false,true", "NOT_STARTED,true,false", "NOT_STARTED,false,false",
                    "SUCCEEDED,true,true", "SUCCEEDED,false,true", "SUCCEEDED,true,false", "SUCCEEDED,false,false",
                    "FAILED,true,true", "FAILED,false,true", "FAILED,true,false", "FAILED,false,false",
                    "NEW_USER,true,true", "NEW_USER,false,true", "NEW_USER,true,false", "NEW_USER,false,false",
                    "ALREADY_MIGRATED,true,true", "ALREADY_MIGRATED,false,true", "ALREADY_MIGRATED,true,false", "ALREADY_MIGRATED,false,false",
            })
            void whenPrivacyPolicyAccepted_MigrateAdminUser(final MigrationStatus migrationStatus, final String migrateFindEnabled, final boolean hasColaSub) {
                final PrivacyPolicyDto privacyPolicyDto = PrivacyPolicyDto.builder().acceptPrivacyPolicy("yes").build();
                final BindingResult result = Mockito.mock(BindingResult.class);
                final MockHttpServletRequest request = new MockHttpServletRequest();
                final String redirectUrl = "redirectUrl";
                final User user = userBuilder
                        .roles(List.of(Role.builder().name(RoleEnum.ADMIN).build()))
                        .loginJourneyState(LoginJourneyState.PRIVACY_POLICY_PENDING)
                        .colaSub(hasColaSub ? UUID.randomUUID() : null)
                        .applyAccountMigrated(migrationStatus)
                        .findAccountMigrated(migrationStatus)
                        .build();

                ReflectionTestUtils.setField(loginController, "findAccountsMigrationEnabled", migrateFindEnabled);
                mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                        .thenReturn(new Cookie("userServiceCookieName", mockJwt));
                when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());
                when(result.hasErrors()).thenReturn(false);
                when(oneLoginUserService.getUserFromSub(anyString())).thenReturn(Optional.of(user));

                final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto, result, request, Optional.of(redirectUrl));

                assertThat(methodResponse.getViewName()).isEqualTo("redirect:http:localhost:3000/adminBaseUrl?redirectUrl=%2Fdashboard%3FapplyMigrationStatus%3D" + migrationStatus + "%26findMigrationStatus%3D" + migrationStatus);
                if (hasColaSub) {
                    verify(oneLoginUserService, times(1)).migrateApplyUser(user, mockJwt);
                } else {
                    verify(oneLoginUserService, times(0)).migrateApplyUser(user, mockJwt);
                }
                if (migrateFindEnabled.equals("true")) {
                    verify(oneLoginUserService, times(1)).migrateFindUser(user, mockJwt);
                    verify(oneLoginUserService, times(1)).setUsersLoginJourneyState(user, LoginJourneyState.USER_MIGRATED_AND_READY);
                    verify(oneLoginUserService, times(0)).setUsersLoginJourneyState(user, LoginJourneyState.USER_READY);
                } else {
                    verify(oneLoginUserService, times(0)).migrateFindUser(user, mockJwt);
                    verify(oneLoginUserService, times(0)).setUsersLoginJourneyState(user, LoginJourneyState.USER_MIGRATED_AND_READY);
                    verify(oneLoginUserService, times(1)).setUsersLoginJourneyState(user, LoginJourneyState.USER_READY);
                }
            }
        }

        @Nested
        class MigratingFindEmails {
            @Test
            void willShowUpdatedEmailPage_whenUserIsFound() {
                final MockHttpServletRequest request = new MockHttpServletRequest();
                final String redirectUrl = "redirectUrl";
                final User user = User.builder()
                        .sub("sub")
                        .loginJourneyState(LoginJourneyState.USER_READY)
                        .roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build()))
                        .emailAddress("email")
                        .build();

                mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                        .thenReturn(new Cookie("userServiceCookieName", mockJwt));
                when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());
                when(oneLoginUserService.getUserFromSub(anyString())).thenReturn(Optional.of(user));


                final ModelAndView methodResponse = loginController.updatedEmailPage(request, Optional.of(redirectUrl));
                assertThat(methodResponse.getViewName()).isEqualTo(LoginControllerV2.UPDATED_EMAIL_PAGE_VIEW);
                assertThat(methodResponse.getModel()).containsEntry("email", user.getEmailAddress());
            }

            @Test
            void willThrowException_whenUserIsNotFound() {
                final MockHttpServletRequest request = new MockHttpServletRequest();
                final String redirectUrl = "redirectUrl";

                mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                        .thenReturn(new Cookie("userServiceCookieName", mockJwt));
                doThrow(new UserNotFoundException("User not found")).when(oneLoginUserService).getUserFromSub(anyString());
                assertThrows(UserNotFoundException.class, () -> loginController.updatedEmailPage(request, Optional.of(redirectUrl)));
            }

            @ParameterizedTest
            @CsvSource({
                    "USER_READY,true,true", "USER_READY,false,true", "USER_MIGRATED_AND_READY,true,true", "USER_MIGRATED_AND_READY,false,true",
                    "USER_READY,true,false", "USER_READY,false,false", "USER_MIGRATED_AND_READY,true,false", "USER_MIGRATED_AND_READY,false,false"
            })
            void loginAndEmailChanged(final LoginJourneyState initialState, final String migrateFindEnabled, final boolean hasEmailChanged) {
                final String customToken = "a-custom-valid-token";
                final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
                final User user = userBuilder
                        .roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build()))
                        .loginJourneyState(initialState)
                        .emailAddress("oldEmail")
                        .applyAccountMigrated(MigrationStatus.SUCCEEDED)
                        .findAccountMigrated(MigrationStatus.SUCCEEDED)
                        .build();
                final JSONObject tokenResponse = new JSONObject();
                tokenResponse.put("id_token", idToken).put("access_token", accessToken);
                final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                        .emailAddress("newEmail")
                        .sub("sub")
                        .build();
                final boolean migratingUsersFindAccount = initialState.equals(LoginJourneyState.USER_READY) && migrateFindEnabled.equals("true");

                ReflectionTestUtils.setField(loginController, "findAccountsMigrationEnabled", migrateFindEnabled);
                when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
                when(oneLoginService.getOneLoginUserInfoDto(accessToken)).thenReturn(oneLoginUserInfoDto);
                when(oneLoginUserService.createOrGetUserFromInfo(any())).thenReturn(user);
                when(oneLoginService.decodeTokenId(any())).thenReturn(idTokenDtoBuilder.build());
                when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());
                if (hasEmailChanged && !migratingUsersFindAccount) {
                    when(oneLoginUserService.hasEmailChanged(any(), any())).thenReturn(true);
                }

                final RedirectView methodResponse = loginController.redirectAfterLogin(stateCookie, request, response, code, state);

                if (migratingUsersFindAccount) {
                    assertThat(methodResponse.getUrl()).isEqualTo("http://redirectUrl.com?applyMigrationStatus=SUCCEEDED&findMigrationStatus=SUCCEEDED");
                } else {
                    if (hasEmailChanged) {
                        assertThat(methodResponse.getUrl()).isEqualTo("updated-email");
                    } else {
                        assertThat(methodResponse.getUrl()).isEqualTo("http://redirectUrl.com");
                    }
                }
            }
        }
    }

    @Nested
    class logout {
        @Test
        void testLogoutWithBlankCookie() {
            String userServiceCookieName = "customJWT";
            String postLogoutUri = "http:localhost:3002/postLogoutRedirectUri";

            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);
            mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName")).thenReturn(new Cookie(userServiceCookieName, ""));
            RedirectView methodResponse = loginController.logout(request, response);

            verify(oneLoginService, never()).logoutUser(any(Cookie.class), any(HttpServletResponse.class));
            Assertions.assertEquals(postLogoutUri, methodResponse.getUrl());
        }

        @Test
        void testLogoutWithNonBlankCookie() {
            String userServiceCookieName = "customJWT";
            String applicantBaseUrl = "/applicantBaseUrl";

            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);
            mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName")).thenReturn(new Cookie(userServiceCookieName, "ba"));
            when(oneLoginService.logoutUser(any(), any())).thenReturn(new RedirectView(applicantBaseUrl));

            RedirectView methodResponse = loginController.logout(request, response);

            verify(oneLoginService, times(1)).logoutUser(any(Cookie.class), any(HttpServletResponse.class));
            Assertions.assertEquals(applicantBaseUrl, methodResponse.getUrl());
        }
    }

    @Test
    void testValidateSessionsRoles() {
        String emailAddress = "test@email.com";
        String roles = "[FIND, APPLY]";
        ValidateSessionsRolesRequestBodyDto requestBodyDto = new ValidateSessionsRolesRequestBodyDto(emailAddress, roles);
        ResponseEntity<Boolean> response = loginController.validateSessionsRoles(requestBodyDto);
        assertThat(response).isEqualTo(ResponseEntity.ok(Boolean.TRUE));
    }

    @Test
    void testValidateSessionsRolesWithInvalidSession() {
        String emailAddress = "test@email.com";
        String roles = "[FIND, APPLY]";
        ValidateSessionsRolesRequestBodyDto requestBodyDto = new ValidateSessionsRolesRequestBodyDto(emailAddress, roles);
        doThrow(UnauthorizedException.class).when(oneLoginUserService).validateSessionsRoles(emailAddress, roles);
        assertThrows(UnauthorizedException.class, () -> loginController.validateSessionsRoles(requestBodyDto));
    }
}