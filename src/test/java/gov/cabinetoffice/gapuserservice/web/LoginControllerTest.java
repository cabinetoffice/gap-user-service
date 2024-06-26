package gov.cabinetoffice.gapuserservice.web;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import gov.cabinetoffice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetoffice.gapuserservice.config.ThirdPartyAuthProviderProperties;
import gov.cabinetoffice.gapuserservice.exceptions.TokenNotValidException;
import gov.cabinetoffice.gapuserservice.model.Role;
import gov.cabinetoffice.gapuserservice.model.RoleEnum;
import gov.cabinetoffice.gapuserservice.model.User;
import gov.cabinetoffice.gapuserservice.service.JwtBlacklistService;
import gov.cabinetoffice.gapuserservice.service.jwt.impl.ColaJwtServiceImpl;
import gov.cabinetoffice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import gov.cabinetoffice.gapuserservice.util.WebUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private ColaJwtServiceImpl thirdPartyJwtService;

    @Mock
    private CustomJwtServiceImpl customJwtService;

    @Mock
    private JwtBlacklistService jwtBlacklistService;

    private LoginController controllerUnderTest;
    private ThirdPartyAuthProviderProperties authenticationProvider;
    private ApplicationConfigProperties configProperties;

    @BeforeEach
    void setup() {
        authenticationProvider = ThirdPartyAuthProviderProperties.builder()
                .url("https://some-authentication-providder.com")
                .tokenCookie("find-grants.-test")
                .logoutUrl("logout-url")
                .build();

        configProperties = ApplicationConfigProperties.builder()
                .defaultRedirectUrl("https://www.find-government-grants.service.gov.uk/")
                .build();

        controllerUnderTest = new LoginController(authenticationProvider, configProperties, thirdPartyJwtService, customJwtService, jwtBlacklistService);
    }

    @Test
    void loginShouldRedirectToCola_IfTokenIsNull() {
        final Optional<String> redirectUrl = Optional.of("https://www.find-government-grants.service.gov.uk/");
        final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

        final RedirectView methodeResponse = controllerUnderTest.login(null, redirectUrl, response);

        final Cookie redirectUrlCookie = new Cookie(LoginController.REDIRECT_URL_COOKIE, redirectUrl.get());
        redirectUrlCookie.setSecure(true);
        redirectUrlCookie.setHttpOnly(true);
        redirectUrlCookie.setPath("/");

        verify(response).addCookie(redirectUrlCookie);
        assertThat(methodeResponse.getUrl()).isEqualTo(authenticationProvider.getUrl());
    }

    @Test
    void loginShouldReturnRedirectUrl_IfOneIsProvided_AndTokenIsValid() {
        final String customToken = "a-custom-valid-token";
        final Optional<String> redirectUrl = Optional.of("https://www.find-government-grants.service.gov.uk/");
        final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

        when(customJwtService.isTokenValid(customToken))
                .thenReturn(true);

        final RedirectView methodeResponse = controllerUnderTest.login(customToken, redirectUrl, response);

        verify(customJwtService, times(0)).generateToken(any(), eq(false));
        assertThat(methodeResponse.getUrl()).isEqualTo(redirectUrl.get());
    }

    @Test
    void loginShouldReturnDefaultRedirectUrl_IfRedirectUrlNotProvided_AndTokenIsValid() {
        final String customToken = "a-custom-valid-token";
        final Optional<String> redirectUrl = Optional.empty();
        final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

        when(customJwtService.isTokenValid(customToken))
                .thenReturn(true);

        final RedirectView methodeResponse = controllerUnderTest.login(customToken, redirectUrl, response);

        verify(customJwtService, times(0)).generateToken(any(), eq(false));
        assertThat(methodeResponse.getUrl()).isEqualTo(configProperties.getDefaultRedirectUrl());
    }

    @Test
    void redirectAfterColaLogin_ThrowsTokenNotValidException_IfTokenIsNull() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
        final Optional<String> redirectUrl = Optional.of("https://www.find-government-grants.service.gov.uk/");

        assertThrows(TokenNotValidException.class,
                () -> controllerUnderTest.redirectAfterColaLogin(redirectUrl, request, response));
    }

    @Test
    void redirectAfterColaLogin_ThrowsTokenNotValidException_IfTokenIsInvalid() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(authenticationProvider.getTokenCookie(), "a-token"));

        final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
        final Optional<String> redirectUrl = Optional.of("https://www.find-government-grants.service.gov.uk/");

        when(thirdPartyJwtService.isTokenValid(any()))
                .thenReturn(false);

        assertThrows(TokenNotValidException.class,
                () -> controllerUnderTest.redirectAfterColaLogin(redirectUrl, request, response));
    }

    @Test
    void redirectAfterColaLogin_RedirectsToLoginEndpoint() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final String cookieValue = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        final DecodedJWT jwt = JWT.decode(cookieValue);
        request.setCookies(new Cookie(authenticationProvider.getTokenCookie(), cookieValue));

        final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
        final Optional<String> redirectUrl = Optional.of("https://www.find-government-grants.service.gov.uk/");
        final String token = "a-generated-token";

        final String authenticationCookieDomain = Objects.equals(this.configProperties.getProfile(), "LOCAL") ? "localhost" : "cabinetoffice.gov.uk";
        final Cookie thirdPartyAuthToken = WebUtil.buildCookie(
                new Cookie(authenticationProvider.getTokenCookie(), null),
                Boolean.TRUE,
                Boolean.TRUE,
                authenticationCookieDomain
        );
        thirdPartyAuthToken.setMaxAge(0);

        when(thirdPartyJwtService.isTokenValid(any()))
                .thenReturn(true);
        when(thirdPartyJwtService.decodeJwt(any())).thenReturn(jwt);
        when(customJwtService.generateToken(any(), eq(false)))
                .thenReturn(token);

        final RedirectView methodeResponse = controllerUnderTest.redirectAfterColaLogin(redirectUrl, request, response);

        verify(response, atLeastOnce()).addCookie(thirdPartyAuthToken);
        assertThat(methodeResponse.getUrl()).isEqualTo(redirectUrl.get());
    }

    @Test
    void validateUser_NullJwt() {
        final ResponseEntity<Boolean> response = controllerUnderTest.validateUser(null);
        assertThat(response.getBody()).isFalse();
    }

    @Test
    void validateUser_JwtNotValid() {
        final String invalidOrExpiredToken = "an-invalid-or-expired-token";

        when(customJwtService.isTokenValid(invalidOrExpiredToken)).thenReturn(false);

        final ResponseEntity<Boolean> response = controllerUnderTest.validateUser(invalidOrExpiredToken);

        verify(customJwtService).isTokenValid(invalidOrExpiredToken);
        assertThat(response.getBody()).isFalse();
    }

    @Test
    void validateUser_JwtValid() {
        final String validToken = "a-valid-token";

        when(customJwtService.isTokenValid(validToken)).thenReturn(true);

        final ResponseEntity<Boolean> response = controllerUnderTest.validateUser(validToken);

        verify(customJwtService).isTokenValid(validToken);
        assertThat(response.getBody()).isTrue();
    }

    @Test
    void logout_RemovesTokenFromCookies() {
        final String customToken = "a-custom-valid-token";
        final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

        controllerUnderTest.logout(customToken, response);

        final Cookie userServiceCookie = WebUtil.buildCookie(
                new Cookie(LoginController.USER_SERVICE_COOKIE_NAME, null),
                Boolean.TRUE,
                Boolean.TRUE,
                null
        );
        userServiceCookie.setMaxAge(0);

        final String authenticationCookieDomain = Objects.equals(this.configProperties.getProfile(), "LOCAL") ? "localhost" : "cabinetoffice.gov.uk";
        final Cookie thirdPartyAuthToken = WebUtil.buildCookie(
                new Cookie(authenticationProvider.getTokenCookie(), null),
                Boolean.TRUE,
                Boolean.TRUE,
                authenticationCookieDomain
        );
        thirdPartyAuthToken.setMaxAge(0);

        verify(response, atLeastOnce()).addCookie(userServiceCookie);
        verify(response, atLeastOnce()).addCookie(thirdPartyAuthToken);
    }

    @Test
    void logout_RedirectsToColaLogout() {
        final String customToken = "a-custom-valid-token";
        final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

        final RedirectView methodeResponse = controllerUnderTest.logout(customToken, response);

        assertThat(methodeResponse.getUrl()).isEqualTo(authenticationProvider.getLogoutUrl());
    }

    @Test
    void refreshToken_ShouldAddExistingTokenToBlackList_AndReturnAFreshOne() {

        final String existingToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        final String redirectUrl = "any-url";
        final String refreshedToken = "a-refreshed-token";
        final Cookie userTokenCookie = new Cookie(LoginController.USER_SERVICE_COOKIE_NAME, refreshedToken);
        userTokenCookie.setSecure(true);
        userTokenCookie.setHttpOnly(true);
        userTokenCookie.setPath("/");

        final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
        final HttpServletRequest request = Mockito.spy(new MockHttpServletRequest());

        when(customJwtService.generateToken(any(), eq(true))).thenReturn(refreshedToken);
        when(customJwtService.getUserFromJwt(request)).thenReturn(Optional.of(User.builder().roles(List.of(Role.builder().name(RoleEnum.ADMIN).build())).build()));

        final RedirectView methodResponse = controllerUnderTest.refreshToken(existingToken, request, response, redirectUrl);

        verify(jwtBlacklistService, atLeastOnce()).addJwtToBlacklist(existingToken);
        verify(response).addCookie(userTokenCookie);

        assertThat(methodResponse.getUrl()).isEqualTo(redirectUrl);
    }
}
