package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetofice.gapuserservice.dto.OneLoginUserInfoDto;
import gov.cabinetofice.gapuserservice.model.Department;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.OneLoginService;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.WebUtils;

import java.util.HashMap;
import java.sql.Ref;
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
    private CustomJwtServiceImpl customJwtService;

    private static MockedStatic<WebUtils> mockedStatic;

    @BeforeEach
    void setUp() {
        mockedStatic = mockStatic(WebUtils.class);

        configProperties = ApplicationConfigProperties.builder()
                .defaultRedirectUrl("https://www.find-government-grants.service.gov.uk/")
                .build();

        loginController = new LoginControllerV2(oneLoginService, customJwtService, configProperties);
        ReflectionTestUtils.setField(loginController, "oneLoginBaseUrl", "oneLoginBaseUrl");
        ReflectionTestUtils.setField(loginController, "userServiceCookieName", "userServiceCookieName");
        ReflectionTestUtils.setField(loginController, "adminBaseUrl", "adminBaseUrl");
    }

    @AfterEach
    public void close() {
        mockedStatic.close();
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

            mockedStatic.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
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

            mockedStatic.when(() -> WebUtils.getCookie(request, "userServiceCookieName"))
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

        final static Cookie customJwtCookie = new Cookie("userServiceCookieName", "a-custom-valid-token");

        @BeforeAll
        static void beforeAll() {
            customJwtCookie.setSecure(true);
            customJwtCookie.setHttpOnly(true);
            customJwtCookie.setPath("/");
        }

        @BeforeEach
        void beforeEach() {
            when(oneLoginService.getUserInfo(null))
                    .thenReturn(OneLoginUserInfoDto.builder()
                            .sub("sub")
                            .emailAddress("email")
                            .build());
            when(customJwtService.generateToken(any()))
                    .thenReturn("a-custom-valid-token");
        }

        @Test
        void shouldCreateNewUser_WhenNoUserFound() {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final Optional<String> redirectUrl = Optional.of("privacy-policy");

            when(oneLoginService.getUser("email", "sub"))
                    .thenReturn(Optional.empty());
            when(oneLoginService.createUser("sub", "email"))
                    .thenReturn(User.builder().acceptedPrivacyPolicy(false).build());
            when(oneLoginService.getNewUserRoles())
                    .thenReturn(List.of(RoleEnum.APPLICANT, RoleEnum.FIND));

            final RedirectView methodResponse = loginController.redirectAfterLogin(redirectUrl, response, "a-custom-valid-token");

            verify(oneLoginService).createUser("sub", "email");
            assertThat(methodResponse.getUrl()).isEqualTo(redirectUrl.get());

            verify(response).addCookie(customJwtCookie);
            final Map<String, String> claims = new HashMap<>();
            claims.put("sub", "sub");
            claims.put("email", "email");
            claims.put("roles", "[APPLICANT, FIND]");
            verify(customJwtService).generateToken(claims);
        }

        @Test
        void shouldDoNothing_WhenUserFoundWithSub() {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final Optional<String> redirectUrl = Optional.of("redirectUrl");
            final User user = User.builder().sub("sub").emailAddress("email").acceptedPrivacyPolicy(true).roles(List.of(
                    Role.builder().name(RoleEnum.APPLICANT).build()
            )).build();

            when(oneLoginService.getUser("email", "sub"))
                    .thenReturn(Optional.of(user));

            final RedirectView methodResponse = loginController.redirectAfterLogin(redirectUrl, response, "a-custom-valid-token");

            verify(oneLoginService, times(0)).createUser(anyString(), anyString());
            verify(oneLoginService, times(0)).addSubToUser(anyString(), anyString());
            assertThat(methodResponse.getUrl()).isEqualTo(redirectUrl.get());

            verify(response).addCookie(customJwtCookie);
            final Map<String, String> claims = new HashMap<>();
            claims.put("sub", "sub");
            claims.put("email", "email");
            claims.put("roles", "[APPLICANT]");
            verify(customJwtService).generateToken(claims);
        }

        @Test
        void shouldUpdateUser_WhenUserFoundWithoutSub_AndIsAdmin() {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final Optional<String> redirectUrl = Optional.of("redirectUrl");
            final User user = User.builder().emailAddress("email").acceptedPrivacyPolicy(true).roles(List.of(
                    Role.builder().name(RoleEnum.ADMIN).build(),
                    Role.builder().name(RoleEnum.APPLICANT).build(),
                    Role.builder().name(RoleEnum.FIND).build()
            )).department(Department.builder().name("department").build()).
                    build();

            when(oneLoginService.getUser("email", "sub"))
                    .thenReturn(Optional.of(user));

            final RedirectView methodResponse = loginController.redirectAfterLogin(redirectUrl, response, "a-custom-valid-token");

            verify(oneLoginService).addSubToUser("sub", "email");
            assertThat(methodResponse.getUrl()).isEqualTo(redirectUrl.get());

            final Cookie redirectUrlCookie = new Cookie(LoginController.REDIRECT_URL_COOKIE, "adminBaseUrl" + "?redirectUrl=/dashboard");
            redirectUrlCookie.setSecure(true);
            redirectUrlCookie.setHttpOnly(true);
            redirectUrlCookie.setPath("/");
            verify(response).addCookie(redirectUrlCookie);


            verify(response).addCookie(customJwtCookie);
            final Map<String, String> claims = new HashMap<>();
            claims.put("sub", "sub");
            claims.put("email", "email");
            claims.put("roles", "[ADMIN, APPLICANT, FIND]");
            claims.put("department", "department");
            verify(customJwtService).generateToken(claims);
        }

        @Test
        void shouldUpdateUser_WhenUserFoundWithoutSub_AndIsSuperAdmin() {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final Optional<String> redirectUrl = Optional.of("redirectUrl");
            final User user = User.builder().emailAddress("email").acceptedPrivacyPolicy(true).roles(List.of(
                    Role.builder().name(RoleEnum.SUPER_ADMIN).build(),
                    Role.builder().name(RoleEnum.ADMIN).build(),
                    Role.builder().name(RoleEnum.APPLICANT).build(),
                    Role.builder().name(RoleEnum.FIND).build()
            )).build();

            when(oneLoginService.getUser("email", "sub"))
                    .thenReturn(Optional.of(user));

            final RedirectView methodResponse = loginController.redirectAfterLogin(redirectUrl, response, "a-custom-valid-token");

            verify(oneLoginService).addSubToUser("sub", "email");
            assertThat(methodResponse.getUrl()).isEqualTo(redirectUrl.get());

            final Cookie redirectUrlCookie = new Cookie(LoginController.REDIRECT_URL_COOKIE, "adminBaseUrl" + "?redirectUrl=/super-admin-dashboard");
            redirectUrlCookie.setSecure(true);
            redirectUrlCookie.setHttpOnly(true);
            redirectUrlCookie.setPath("/");
            verify(response).addCookie(redirectUrlCookie);

            verify(response).addCookie(customJwtCookie);
            final Map<String, String> claims = new HashMap<>();
            claims.put("sub", "sub");
            claims.put("email", "email");
            claims.put("roles", "[SUPER_ADMIN, ADMIN, APPLICANT, FIND]");
            verify(customJwtService).generateToken(claims);
        }

        @Test
        void shouldGoToMigrateDataPage_WhenUserFoundWithoutSub_AndIsAnApplicant() {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final Optional<String> redirectUrl = Optional.of("redirectUrl");
            final User user = User.builder().emailAddress("email").roles(List.of(
                    Role.builder().name(RoleEnum.APPLICANT).build(),
                    Role.builder().name(RoleEnum.FIND).build()
            )).build();

            when(oneLoginService.getUser("email", "sub"))
                    .thenReturn(Optional.of(user));

            final RedirectView methodResponse = loginController.redirectAfterLogin(redirectUrl, response, "a-custom-valid-token");

            assertThat(methodResponse.getUrl()).isEqualTo("/should-migrate-data");

            verify(response).addCookie(customJwtCookie);
            final Map<String, String> claims = new HashMap<>();
            claims.put("sub", "sub");
            claims.put("email", "email");
            claims.put("roles", "[APPLICANT, FIND]");
            verify(customJwtService).generateToken(claims);
        }

        @Test
        void shouldRedirectToPolicyPage_WhenUserHasNotAccepted() {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final Optional<String> redirectUrl = Optional.of("redirectUrl");
            final User user = User.builder().emailAddress("email").acceptedPrivacyPolicy(false).roles(List.of(
                            Role.builder().name(RoleEnum.ADMIN).build(),
                            Role.builder().name(RoleEnum.APPLICANT).build(),
                            Role.builder().name(RoleEnum.FIND).build()
                    )).department(Department.builder().name("department").build()).
                    build();

            when(oneLoginService.getUser("email", "sub"))
                    .thenReturn(Optional.of(user));

            final RedirectView methodResponse = loginController.redirectAfterLogin(redirectUrl, response, "a-custom-valid-token");
            assertThat(methodResponse.getUrl()).isEqualTo("privacy-policy");
        }

    }
}
