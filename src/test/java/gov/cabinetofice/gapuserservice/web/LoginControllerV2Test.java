package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetofice.gapuserservice.config.FindAGrantConfigProperties;
import gov.cabinetofice.gapuserservice.dto.OneLoginUserInfoDto;
import gov.cabinetofice.gapuserservice.dto.PrivacyPolicyDto;
import gov.cabinetofice.gapuserservice.enums.LoginJourneyState;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.OneLoginService;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import gov.cabinetofice.gapuserservice.util.WebUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.WebUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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
        @Test
        void shouldRedirectToLoginPage_IfTokenIsNull_AndMigrationJourneyDisabled() {
            ReflectionTestUtils.setField(loginController, "migrationEnabled", "false");
            final Optional<String> redirectUrl = Optional.of("https://www.find-government-grants.service.gov.uk/");
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final MockHttpServletRequest request = new MockHttpServletRequest();
            when(oneLoginService.getOneLoginAuthorizeUrl())
                    .thenReturn("loginUrl");

            final RedirectView methodResponse = loginController.login(redirectUrl, request, response);

            final Cookie redirectUrlCookie = new Cookie(LoginController.REDIRECT_URL_COOKIE, redirectUrl.get());
            redirectUrlCookie.setSecure(true);
            redirectUrlCookie.setHttpOnly(true);
            redirectUrlCookie.setPath("/");

            verify(response).addCookie(redirectUrlCookie);
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
    }

    @Nested
    class redirectAfterLogin {

        final String redirectUrlCookie = "redirectUrl";
        final String code = "code";
        final User.UserBuilder userBuilder = User.builder()
                .emailAddress("email")
                .sub("sub")
                .loginJourneyState(LoginJourneyState.PRIVACY_POLICY_PENDING)
                .roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build()));

        @Test
        void shouldFetchOneLoginUserInfo() throws JSONException {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

            when(oneLoginService.createOrGetUserFromInfo(any())).thenReturn(userBuilder.build());
            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(new JSONObject("{access_token: code, id_token: code}"));

            loginController.redirectAfterLogin(redirectUrlCookie, response, code);

            verify(oneLoginService).getOneLoginUserInfoDto(code);
        }

        @Test
        void shouldCreateOrGetUserFromOneLoginInfo() throws JSONException {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                    .emailAddress("email")
                    .sub("sub")
                    .build();

            when(oneLoginService.getOneLoginUserInfoDto(code)).thenReturn(oneLoginUserInfoDto);
            when(oneLoginService.createOrGetUserFromInfo(oneLoginUserInfoDto)).thenReturn(userBuilder.build());
            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(new JSONObject("{access_token: code, id_token: code}"));

            loginController.redirectAfterLogin(redirectUrlCookie, response, code);

            verify(oneLoginService).createOrGetUserFromInfo(oneLoginUserInfoDto);
        }

        @Test
        void shouldCreateJwtCookie() throws JSONException {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final Map<String, String> claims = Map.of("claim1", "value1", "claim2", "value2");
            final Cookie cookie = WebUtil.buildSecureCookie("userServiceCookieName", "jwtToken");

            when(oneLoginService.createOrGetUserFromInfo(any())).thenReturn(userBuilder.build());
            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(new JSONObject("{access_token: string1, id_token: jwtToken}"));
            when(oneLoginService.generateCustomJwtClaims(any(), any())).thenReturn(claims);
            when(customJwtService.generateToken(claims)).thenReturn("jwtToken");
            when(oneLoginService.getOneLoginUserInfoDto(any())).thenReturn(null);

            loginController.redirectAfterLogin(redirectUrlCookie, response, code);

            verify(response).addCookie(cookie);
        }

        @Test
        void shouldRedirectToPrivacyPolicyPage_whenItHasNotBeenAccepted() throws JSONException {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

            when(oneLoginService.createOrGetUserFromInfo(any())).thenReturn(userBuilder.build());
            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(new JSONObject("{access_token: string, id_token: string}"));

            final RedirectView methodResponse = loginController.redirectAfterLogin(redirectUrlCookie, response, code);

            assertThat(methodResponse.getUrl()).isEqualTo("privacy-policy");
        }

        @Test
        void shouldRedirectToSADashboard_whenUserIsSA() throws JSONException {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final User user = userBuilder
                    .roles(List.of(Role.builder().name(RoleEnum.SUPER_ADMIN).build()))
                    .loginJourneyState(LoginJourneyState.USER_READY)
                    .build();

            when(oneLoginService.createOrGetUserFromInfo(any())).thenReturn(user);
            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(new JSONObject("{access_token: string, id_token: string}"));

            final RedirectView methodResponse = loginController.redirectAfterLogin(redirectUrlCookie, response, code);


            assertThat(methodResponse.getUrl()).isEqualTo("/adminBaseUrl?redirectUrl=/super-admin-dashboard");
        }

        @Test
        void shouldRedirectToAdminDashboard_whenUserIsAdmin() throws JSONException {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final User user = userBuilder
                    .roles(List.of(Role.builder().name(RoleEnum.ADMIN).build()))
                    .loginJourneyState(LoginJourneyState.USER_READY)
                    .build();

            when(oneLoginService.createOrGetUserFromInfo(any())).thenReturn(user);
            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(new JSONObject("{access_token: string, id_token: string}"));

            final RedirectView methodResponse = loginController.redirectAfterLogin(redirectUrlCookie, response, code);

            assertThat(methodResponse.getUrl()).isEqualTo("/adminBaseUrl?redirectUrl=/dashboard");
        }

        @Test
        void shouldRedirectToRedirectUrlCookie_whenUserIsApplicant() throws JSONException {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final User user = userBuilder
                    .roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build()))
                    .loginJourneyState(LoginJourneyState.USER_READY)
                    .build();


            when(oneLoginService.createOrGetUserFromInfo(any())).thenReturn(user);
            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(new JSONObject("{access_token: string, id_token: string}"));

            final RedirectView methodResponse = loginController.redirectAfterLogin(redirectUrlCookie, response, code);

            assertThat(methodResponse.getUrl()).isEqualTo(redirectUrlCookie);
        }

        @Test
        void shouldRedirectToTechSupportDashboard_whenUserIsTechSupport() throws JSONException {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final User user = userBuilder
                    .roles(List.of(Role.builder().name(RoleEnum.TECHNICAL_SUPPORT).build()))
                    .loginJourneyState(LoginJourneyState.USER_READY)
                    .build();

            when(oneLoginService.createOrGetUserFromInfo(any())).thenReturn(user);
            when(oneLoginService.getOneLoginUserTokenResponse(code)).thenReturn(new JSONObject("{access_token: string, id_token: string}"));

            final RedirectView methodResponse = loginController.redirectAfterLogin(redirectUrlCookie, response, code);

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

        @Nested
        class logout {
            @Test
            void testLogoutWithBlankCookie() {
                String userServiceCookieName = "customJWT";
                String applicantBaseUrl = "/applicantBaseUrl";

                HttpServletRequest request = mock(HttpServletRequest.class);
                mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName")).thenReturn( new Cookie(userServiceCookieName, "") );
                RedirectView methodResponse = loginController.logout(request);

                verify(oneLoginService, never()).logoutUser(any(Cookie.class));
                Assertions.assertEquals(applicantBaseUrl, methodResponse.getUrl());
            }

            @Test
            void testLogoutWithNonBlankCookie() {
                String userServiceCookieName = "customJWT";
                String applicantBaseUrl = "/applicantBaseUrl";

                HttpServletRequest request = mock(HttpServletRequest.class);
                mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName")).thenReturn( new Cookie(userServiceCookieName, "ba") );

                RedirectView methodResponse = loginController.logout(request);

                verify(oneLoginService, times(1)).logoutUser(any(Cookie.class));
                Assertions.assertEquals(applicantBaseUrl, methodResponse.getUrl());
            }
        }
    }
}
