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
import jakarta.servlet.http.HttpServletResponse;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    }

    @AfterEach
    public void close() {
        mockedWebUtils.close();
    }

    @Nested
    class login {
        @Test
        void shouldRedirectToNoticePage_IfTokenIsNull() {
            final Optional<String> redirectUrl = Optional.of("https://www.find-government-grants.service.gov.uk/");
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final MockHttpServletRequest request = new MockHttpServletRequest();

            final RedirectView methodResponse = loginController.login(redirectUrl, request, response);

            final Cookie redirectUrlCookie = new Cookie(LoginController.REDIRECT_URL_COOKIE, redirectUrl.get());
            redirectUrlCookie.setSecure(true);
            redirectUrlCookie.setHttpOnly(true);
            redirectUrlCookie.setPath("/");

            verify(response).addCookie(redirectUrlCookie);
            assertThat(methodResponse.getUrl()).isEqualTo("notice-page");
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
            when(oneLoginService.getOneLoginAuthorizeUrl())
                    .thenReturn("loginUrl");

            final ModelAndView methodResponse = loginController.showNoticePage();
            assertThat(methodResponse.getViewName()).isEqualTo(LoginControllerV2.NOTICE_PAGE_VIEW);
            assertThat(methodResponse.getModel().get("loginUrl")).isEqualTo("loginUrl");

        }
    }

    @Nested
    class redirectAfterLogin {

        final String redirectUrlCookie = "redirectUrl";
        final String code = "code";
        final User.UserBuilder userBuilder = User.builder()
                .emailAddress("email")
                .sub("sub")
                .loginJourneyState(LoginJourneyState.CREATING_NEW_USER)
                .roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build()))
                .acceptedPrivacyPolicy(false);

        @Test
        void shouldFetchOneLoginUserInfo() {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

            when(oneLoginService.createOrGetUserFromInfo(any())).thenReturn(userBuilder.build());

            loginController.redirectAfterLogin(redirectUrlCookie, response, code);

            verify(oneLoginService).getOneLoginUserInfoDto(code);
        }

        @Test
        void shouldCreateOrGetUserFromOneLoginInfo() {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                    .emailAddress("email")
                    .sub("sub")
                    .build();

            when(oneLoginService.getOneLoginUserInfoDto(code)).thenReturn(oneLoginUserInfoDto);
            when(oneLoginService.createOrGetUserFromInfo(oneLoginUserInfoDto)).thenReturn(userBuilder.build());

            loginController.redirectAfterLogin(redirectUrlCookie, response, code);

            verify(oneLoginService).createOrGetUserFromInfo(oneLoginUserInfoDto);
        }

        @Test
        void shouldCreateJwtCookie() {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final Map<String, String> claims = Map.of("claim1", "value1", "claim2", "value2");
            final Cookie cookie = WebUtil.buildSecureCookie("userServiceCookieName", "jwtToken");

            when(oneLoginService.createOrGetUserFromInfo(any())).thenReturn(userBuilder.build());
            when(oneLoginService.generateCustomJwtClaims(any())).thenReturn(claims);
            when(customJwtService.generateToken(claims)).thenReturn("jwtToken");

            loginController.redirectAfterLogin(redirectUrlCookie, response, code);

            verify(response).addCookie(cookie);
        }

        @Test
        void shouldRedirectToPrivacyPolicyPage_whenItHasNotBeenAccepted() {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

            when(oneLoginService.createOrGetUserFromInfo(any())).thenReturn(userBuilder.build());

            final RedirectView methodResponse = loginController.redirectAfterLogin(redirectUrlCookie, response, code);

            assertThat(methodResponse.getUrl()).isEqualTo("privacy-policy");
        }

        @Test
        void shouldRedirectToSADashboard_whenUserIsSA() {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final User user = userBuilder
                    .roles(List.of(Role.builder().name(RoleEnum.SUPER_ADMIN).build()))
                    .loginJourneyState(LoginJourneyState.USER_READY)
                    .build();

            when(oneLoginService.createOrGetUserFromInfo(any())).thenReturn(user);

            final RedirectView methodResponse = loginController.redirectAfterLogin(redirectUrlCookie, response, code);

            assertThat(methodResponse.getUrl()).isEqualTo("/adminBaseUrl?redirectUrl=/super-admin-dashboard");
        }

        @Test
        void shouldRedirectToAdminDashboard_whenUserIsAdmin() {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final User user = userBuilder
                    .roles(List.of(Role.builder().name(RoleEnum.ADMIN).build()))
                    .loginJourneyState(LoginJourneyState.USER_READY)
                    .build();

            when(oneLoginService.createOrGetUserFromInfo(any())).thenReturn(user);

            final RedirectView methodResponse = loginController.redirectAfterLogin(redirectUrlCookie, response, code);

            assertThat(methodResponse.getUrl()).isEqualTo("/adminBaseUrl?redirectUrl=/dashboard");
        }

        @Test
        void shouldRedirectToRedirectUrlCookie_whenUserIsApplicant() {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final User user = userBuilder
                    .roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build()))
                    .loginJourneyState(LoginJourneyState.USER_READY)
                    .build();

            when(oneLoginService.createOrGetUserFromInfo(any())).thenReturn(user);

            final RedirectView methodResponse = loginController.redirectAfterLogin(redirectUrlCookie, response, code);

            assertThat(methodResponse.getUrl()).isEqualTo(redirectUrlCookie);
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
            verify(oneLoginService, times(0)).setPrivacyPolicy(any());
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
                    .acceptedPrivacyPolicy(true)
                    .build();

            mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                    .thenReturn(new Cookie("userServiceCookieName", mockJwt));
            when(result.hasErrors()).thenReturn(false);
            when(oneLoginService.getUserFromSub(anyString())).thenReturn(Optional.of(user));
            when(oneLoginService.setPrivacyPolicy(user)).thenReturn(user);

            final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto, result, request, redirectUrl);

            assertThat(methodResponse.getViewName()).isEqualTo("redirect:" + redirectUrl);
            verify(oneLoginService).setPrivacyPolicy(user);
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
                    .acceptedPrivacyPolicy(true)
                    .build();

            mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                    .thenReturn(new Cookie("userServiceCookieName", mockJwt));
            when(result.hasErrors()).thenReturn(false);
            when(oneLoginService.getUserFromSub(anyString())).thenReturn(Optional.of(user));
            when(oneLoginService.setPrivacyPolicy(user)).thenReturn(user);

            final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto, result, request, redirectUrl);

            assertThat(methodResponse.getViewName()).isEqualTo("redirect:/adminBaseUrl?redirectUrl=/dashboard");
            verify(oneLoginService).setPrivacyPolicy(user);
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
                    .acceptedPrivacyPolicy(true)
                    .build();

            mockedWebUtils.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
                    .thenReturn(new Cookie("userServiceCookieName", mockJwt));
            when(result.hasErrors()).thenReturn(false);
            when(oneLoginService.getUserFromSub(anyString())).thenReturn(Optional.of(user));
            when(oneLoginService.setPrivacyPolicy(user)).thenReturn(user);

            final ModelAndView methodResponse = loginController.submitToPrivacyPolicyPage(privacyPolicyDto, result, request, redirectUrl);

            assertThat(methodResponse.getViewName()).isEqualTo("redirect:/adminBaseUrl?redirectUrl=/super-admin-dashboard");
            verify(oneLoginService).setPrivacyPolicy(user);
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.PRIVACY_POLICY_ACCEPTED);
            verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.USER_READY);
        }
    }
}
