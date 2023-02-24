package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetofice.gapuserservice.config.ThirdPartyAuthProviderProperties;
import gov.cabinetofice.gapuserservice.exceptions.TokenNotValidException;
import gov.cabinetofice.gapuserservice.service.jwt.impl.ColaJwtServiceImpl;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private ColaJwtServiceImpl thirdPartyJwtService;

    @Mock
    private CustomJwtServiceImpl customJwtService;

    private LoginController controllerUnderTest;
    private ThirdPartyAuthProviderProperties authenticationProvider;
    private ApplicationConfigProperties configProperties;

    @BeforeEach
    void setup() {
        authenticationProvider = ThirdPartyAuthProviderProperties.builder()
                .url("https://some-authentication-providder.com")
                .tokenCookie("find-grants.-test")
                .build();

        configProperties = ApplicationConfigProperties.builder()
                .defaultRedirectUrl("https://www.find-government-grants.service.gov.uk/")
                .build();

        controllerUnderTest = new LoginController(authenticationProvider, configProperties, thirdPartyJwtService, customJwtService);
    }

    @Test
    void loginShouldRedirectToCola_IfTokenIsNull() {
        final Optional<String> redirectUrl = Optional.of("https://www.find-government-grants.service.gov.uk/");
        final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

        final RedirectView methodeResponse = controllerUnderTest.login(null, redirectUrl, response);

        final Cookie redirectUrlCookie = new Cookie(LoginController.REDIRECT_URL_COOKIE, redirectUrl.get());
        redirectUrlCookie.setSecure(true);
        redirectUrlCookie.setHttpOnly(true);

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

        verify(customJwtService, times(0)).generateToken();
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

        verify(customJwtService, times(0)).generateToken();
        assertThat(methodeResponse.getUrl()).isEqualTo(configProperties.getDefaultRedirectUrl());
    }

    @Test
    void redirectAfterColaLogin_ThrowsTokenNotValidException_IfTokenIsNull() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
        final Optional<String> redirectUrl = Optional.of("https://www.find-government-grants.service.gov.uk/");

        assertThrows(TokenNotValidException.class, () -> controllerUnderTest.redirectAfterColaLogin(redirectUrl, request, response));
    }

    @Test
    void redirectAfterColaLogin_ThrowsTokenNotValidException_IfTokenIsInvalid() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie[] {
                new Cookie(authenticationProvider.getTokenCookie(), "a-token")
        });

        final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
        final Optional<String> redirectUrl = Optional.of("https://www.find-government-grants.service.gov.uk/");

        when(thirdPartyJwtService.isTokenValid(any()))
                .thenReturn(false);

        assertThrows(TokenNotValidException.class, () -> controllerUnderTest.redirectAfterColaLogin(redirectUrl, request, response));
    }

    @Test
    void redirectAfterColaLogin_RedirectsToLoginEndpoint() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie[] {
                new Cookie(authenticationProvider.getTokenCookie(), "a-token")
        });

        final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
        final Optional<String> redirectUrl = Optional.of("https://www.find-government-grants.service.gov.uk/");
        final String token = "a-generated-token";

        when(thirdPartyJwtService.isTokenValid(any()))
                .thenReturn(true);
        when(customJwtService.generateToken())
                .thenReturn(token);

        final RedirectView methodeResponse = controllerUnderTest.redirectAfterColaLogin(redirectUrl, request, response);

        assertThat(methodeResponse.getUrl()).isEqualTo(redirectUrl.get());
    }
}