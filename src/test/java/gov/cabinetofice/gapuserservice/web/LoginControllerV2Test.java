package gov.cabinetofice.gapuserservice.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import gov.cabinetofice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetofice.gapuserservice.config.FindAGrantConfigProperties;
import gov.cabinetofice.gapuserservice.dto.IdTokenDto;
import gov.cabinetofice.gapuserservice.dto.OneLoginUserInfoDto;
import gov.cabinetofice.gapuserservice.dto.PrivacyPolicyDto;
import gov.cabinetofice.gapuserservice.dto.StateCookieDto;
import gov.cabinetofice.gapuserservice.enums.LoginJourneyState;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.OneLoginService;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import gov.cabinetofice.gapuserservice.util.WebUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.WebUtils;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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

    private static MockedStatic<WebUtils> mockedWebUtils;

    @BeforeEach
    void setUp() {
        mockedWebUtils = mockStatic(WebUtils.class);

        configProperties = ApplicationConfigProperties.builder()
                .defaultRedirectUrl("https://www.find-government-grants.service.gov.uk/")
                .build();

        loginController = new LoginControllerV2(oneLoginService, customJwtService, configProperties, findProperties);
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
        @Test
        void shouldRedirectToNoticePage_IfTokenIsNull() {
            final Optional<String> redirectUrl = Optional.of("https://www.find-government-grants.service.gov.uk/");
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final MockHttpServletRequest request = new MockHttpServletRequest();

            when(oneLoginService.buildEncodedStateJson(redirectUrl.get(), state)).thenCallRealMethod();
            when(oneLoginService.generateNonce()).thenReturn(nonce);
            when(oneLoginService.generateState()).thenReturn(state);

            final RedirectView methodResponse = loginController.login(redirectUrl, request, response);

            final Cookie nonceCookie = WebUtil.buildSecureCookie(loginController.getNONCE_COOKIE(), nonce, 3600);
            verify(response).addCookie(nonceCookie);

            final Cookie stateCookie = WebUtil.buildSecureCookie(loginController.getSTATE_COOKIE(), "eyJyZWRpcmVjdFVybCI6Imh0dHBzOlwvXC93d3cuZmluZC1nb3Zlcm5tZW50LWdyYW50cy5zZXJ2aWNlLmdvdi51a1wvIiwic3RhdGUiOiJzdGF0ZSJ9", 3600);
            verify(response).addCookie(stateCookie);

            assertThat(methodResponse.getUrl()).isEqualTo("notice-page");
        }

        @Test
        void shouldRedirectToLoginPage_IfTokenIsNull_AndMigrationJourneyDisabled() {
            ReflectionTestUtils.setField(loginController, "migrationEnabled", "false");
            final Optional<String> redirectUrl = Optional.of("https://www.find-government-grants.service.gov.uk/");
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final MockHttpServletRequest request = new MockHttpServletRequest();

            when(oneLoginService.buildEncodedStateJson(redirectUrl.get(), state)).thenCallRealMethod();
            when(oneLoginService.generateNonce()).thenReturn(nonce);
            when(oneLoginService.generateState()).thenReturn(state);
            when(oneLoginService.getOneLoginAuthorizeUrl(state, nonce)).thenReturn("loginUrl");

            final RedirectView methodResponse = loginController.login(redirectUrl, request, response);

            final Cookie nonceCookie = WebUtil.buildSecureCookie(loginController.getNONCE_COOKIE(), "nonce", 3600);
            verify(response).addCookie(nonceCookie);

            final Cookie stateCookie = WebUtil.buildSecureCookie(loginController.getSTATE_COOKIE(), "eyJyZWRpcmVjdFVybCI6Imh0dHBzOlwvXC93d3cuZmluZC1nb3Zlcm5tZW50LWdyYW50cy5zZXJ2aWNlLmdvdi51a1wvIiwic3RhdGUiOiJzdGF0ZSJ9", 3600);
            verify(response).addCookie(stateCookie);

            assertThat(methodResponse.getUrl()).isEqualTo("loginUrl");
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

        @Test
        void showNoticePage_ShowsNoticePage_WithLoginUrl() {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            when(oneLoginService.getOneLoginAuthorizeUrl(state, nonce)).thenReturn("loginUrl");
            when(oneLoginService.generateNonce()).thenReturn(nonce);
            when(oneLoginService.generateState()).thenReturn(state);

            final ModelAndView methodResponse = loginController.showNoticePage(response);
            assertThat(methodResponse.getViewName()).isEqualTo(LoginControllerV2.NOTICE_PAGE_VIEW);
            assertThat(methodResponse.getModel().get("loginUrl")).isEqualTo("loginUrl");

        }
    }

    @Nested
    class redirectAfterLogin {
        //B64 encoded stateCookie containing '{"state":"state","redirectUrl":"redirectUrl"}'
        final String stateCookie = "eyJzdGF0ZSI6InN0YXRlIiwicmVkaXJlY3RVcmwiOiJyZWRpcmVjdFVybCJ9";
        final String nonceCookie = "nonce";
        final String redirectUrlCookie = "redirectUrl";
        final String code = "code";
        final String state = "state";
        final String idToken = "idToken";
        final String accessToken = "accessToken";
        final User.UserBuilder userBuilder = User.builder()
                .emailAddress("email")
                .sub("sub")
                .loginJourneyState(LoginJourneyState.PRIVACY_POLICY_PENDING)
                .roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build()));
        final IdTokenDto.IdTokenDtoBuilder idTokenDtoBuilder = IdTokenDto.builder()
                .nonce("nonce");
        final StateCookieDto.StateCookieDtoBuilder stateCookieDtoBuilder = StateCookieDto.builder()
                .state("state")
                .redirectUrl("redirectUrl");

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
            when(oneLoginService.getDecodedIdToken(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());

            loginController.redirectAfterLogin(stateCookie, nonceCookie, response, code, state);

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
            when(oneLoginService.getDecodedIdToken(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());

            loginController.redirectAfterLogin(stateCookie, nonceCookie, response, code, state);

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
            when(oneLoginService.getDecodedIdToken(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());
            when(oneLoginService.generateCustomJwtClaims(any())).thenReturn(claims);
            when(customJwtService.generateToken(claims)).thenReturn("jwtToken");

            loginController.redirectAfterLogin(stateCookie, nonceCookie, response, code, state);

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
            when(oneLoginService.getDecodedIdToken(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());

            final RedirectView methodResponse = loginController.redirectAfterLogin(stateCookie, nonceCookie, response, code, state);

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
            when(oneLoginService.getDecodedIdToken(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());

            final RedirectView methodResponse = loginController.redirectAfterLogin(stateCookie, nonceCookie, response, code, state);

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
            when(oneLoginService.getDecodedIdToken(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());

            final RedirectView methodResponse = loginController.redirectAfterLogin(stateCookie, nonceCookie, response, code, state);

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
            when(oneLoginService.getDecodedIdToken(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());

            final RedirectView methodResponse = loginController.redirectAfterLogin(stateCookie, nonceCookie, response, code, state);

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

            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(tokenResponse);
            when(oneLoginService.createOrGetUserFromInfo(any())).thenReturn(user);
            when(oneLoginService.getDecodedIdToken(any())).thenReturn(idTokenDtoBuilder.build());
            when(oneLoginService.decodeStateCookie(any())).thenReturn(stateCookieDtoBuilder.build());

            final RedirectView methodResponse = loginController.redirectAfterLogin(stateCookie, nonceCookie, response, code, state);

            assertThat(methodResponse.getUrl()).isEqualTo("/techSupportAppBaseUrl");
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

            final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto, result, request, redirectUrl);
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

            final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto, result, request, redirectUrl);

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

            final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto, result, request, redirectUrl);

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

            final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto, result, request, redirectUrl);

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

            final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto, result, request, redirectUrl);

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

            final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto, result, request, redirectUrl);

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

            final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto, result, request, redirectUrl);

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

            final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto, result, request, redirectUrl);

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

            final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto, result, request, redirectUrl);

            assertThat(methodResponse.getViewName()).isEqualTo("redirect:/applicantBaseUrl/dashboard?migrationStatus=error");
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.PRIVACY_POLICY_ACCEPTED);
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.MIGRATING_USER);
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.MIGRATION_FAILED);
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.USER_READY);
        }
    }
}
