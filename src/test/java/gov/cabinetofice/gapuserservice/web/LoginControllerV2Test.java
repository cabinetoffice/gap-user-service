package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetofice.gapuserservice.dto.OneLoginUserInfoDto;
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
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.WebUtils;

import java.util.List;
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
        void shouldRedirectToOneLogin_IfTokenIsNull() {
            final Optional<String> redirectUrl = Optional.of("https://www.find-government-grants.service.gov.uk/");
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final MockHttpServletRequest request = new MockHttpServletRequest();

            final RedirectView methodResponse = loginController.login(redirectUrl, request, response);

            final Cookie redirectUrlCookie = new Cookie(LoginController.REDIRECT_URL_COOKIE, redirectUrl.get());
            redirectUrlCookie.setSecure(true);
            redirectUrlCookie.setHttpOnly(true);
            redirectUrlCookie.setPath("/");

            verify(response).addCookie(redirectUrlCookie);
            assertThat(methodResponse.getUrl()).isEqualTo("oneLoginBaseUrl");
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
                            .email("email")
                            .build());
            when(customJwtService.generateToken(any()))
                    .thenReturn("a-custom-valid-token");
        }

        @Test
        void shouldCreateNewUser_WhenNoUserFound() {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final Optional<String> redirectUrl = Optional.of("redirectUrl");

            when(oneLoginService.getUser("email", "sub"))
                    .thenReturn(Optional.empty());
            when(oneLoginService.createUser("sub", "email"))
                    .thenReturn(User.builder().build());

            final RedirectView methodResponse = loginController.redirectAfterLogin(redirectUrl, response, "a-custom-valid-token");

            verify(response).addCookie(customJwtCookie);
            verify(oneLoginService).createUser("sub", "email");
            assertThat(methodResponse.getUrl()).isEqualTo(redirectUrl.get());
        }

        @Test
        void shouldDoNothing_WhenUserFoundWithSub() {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final Optional<String> redirectUrl = Optional.of("redirectUrl");
            final User user = User.builder().sub("sub").email("email").build();

            when(oneLoginService.getUser("email", "sub"))
                    .thenReturn(Optional.of(user));

            final RedirectView methodResponse = loginController.redirectAfterLogin(redirectUrl, response, "a-custom-valid-token");

            verify(response).addCookie(customJwtCookie);
            verify(oneLoginService, times(0)).createUser(anyString(), anyString());
            verify(oneLoginService, times(0)).addSubToUser(anyString(), anyString());
            assertThat(methodResponse.getUrl()).isEqualTo(redirectUrl.get());
        }

        @Test
        void shouldUpdateUser_WhenUserFoundWithoutSub_AndIsAdmin() {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final Optional<String> redirectUrl = Optional.of("redirectUrl");
            final User user = User.builder().email("email").roles(List.of(
                    Role.builder().name(RoleEnum.ADMIN).build(),
                    Role.builder().name(RoleEnum.APPLICANT).build(),
                    Role.builder().name(RoleEnum.FIND).build()
            )).build();

            when(oneLoginService.getUser("email", "sub"))
                    .thenReturn(Optional.of(user));

            final RedirectView methodResponse = loginController.redirectAfterLogin(redirectUrl, response, "a-custom-valid-token");

            verify(response).addCookie(customJwtCookie);
            verify(oneLoginService).addSubToUser("sub", "email");
            assertThat(methodResponse.getUrl()).isEqualTo("adminBaseUrl/dashboard");
        }

        @Test
        void shouldUpdateUser_WhenUserFoundWithoutSub_AndIsSuperAdmin() {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final Optional<String> redirectUrl = Optional.of("redirectUrl");
            final User user = User.builder().email("email").roles(List.of(
                    Role.builder().name(RoleEnum.SUPER_ADMIN).build(),
                    Role.builder().name(RoleEnum.ADMIN).build(),
                    Role.builder().name(RoleEnum.APPLICANT).build(),
                    Role.builder().name(RoleEnum.FIND).build()
            )).build();

            when(oneLoginService.getUser("email", "sub"))
                    .thenReturn(Optional.of(user));

            final RedirectView methodResponse = loginController.redirectAfterLogin(redirectUrl, response, "a-custom-valid-token");

            verify(response).addCookie(customJwtCookie);
            verify(oneLoginService).addSubToUser("sub", "email");
            assertThat(methodResponse.getUrl()).isEqualTo("adminBaseUrl/super-admin/dashboard");
        }

        @Test
        void shouldGoToMigrateDataPage_WhenUserFoundWithoutSub_AndIsAnApplicant() {
            final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
            final Optional<String> redirectUrl = Optional.of("redirectUrl");
            final User user = User.builder().email("email").roles(List.of(
                    Role.builder().name(RoleEnum.APPLICANT).build(),
                    Role.builder().name(RoleEnum.FIND).build()
            )).build();

            when(oneLoginService.getUser("email", "sub"))
                    .thenReturn(Optional.of(user));

            final RedirectView methodResponse = loginController.redirectAfterLogin(redirectUrl, response, "a-custom-valid-token");

            verify(response).addCookie(customJwtCookie);
            assertThat(methodResponse.getUrl()).isEqualTo("/should-migrate-data");
        }
    }
}
