package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.config.ThirdPartyAuthProviderProperties;
import gov.cabinetofice.gapuserservice.service.ThirdPartyJwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.view.RedirectView;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    private ThirdPartyAuthProviderProperties authenticationProvider;
    @Mock
    private ThirdPartyJwtService thirdPartyJwtService;
    private LoginController controllerUnderTest;

    private final String successfulRedirectUrl = "/redirect-after-login";
    private final String authenticationProviderUrl = "https://some-authentication-providder.com";

    @BeforeEach
    void setup() {
        authenticationProvider = ThirdPartyAuthProviderProperties.builder()
                .url(authenticationProviderUrl)
                .build();

        controllerUnderTest = new LoginController(authenticationProvider, thirdPartyJwtService);
    }

    @Test
    void login_redirectsToAuthProvider_IfNoTokenProvided() {

        final String redirectUrl = "https://www.find-government-grants.service.gov.uk/";
        final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

        final RedirectView methodeResponse = controllerUnderTest.login(null, redirectUrl, response);

        verify(response).addCookie(new Cookie("redirectUrl", redirectUrl));
        assertThat(methodeResponse.getUrl()).isEqualTo(authenticationProviderUrl);
    }

    @Test
    void login_redirectsToAuthProvider_IfTokenIsNotValid() {

        final String invalidOrExpiredToken = "an-invalid-or-expired-token";
        final String redirectUrl = "https://www.find-government-grants.service.gov.uk/";
        final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

        when(thirdPartyJwtService.isTokenValid(invalidOrExpiredToken)).thenReturn(false);

        final RedirectView methodeResponse = controllerUnderTest.login(invalidOrExpiredToken, redirectUrl, response);

        verify(response).addCookie(new Cookie("redirectUrl", redirectUrl));
        verify(thirdPartyJwtService).isTokenValid(invalidOrExpiredToken);
        assertThat(methodeResponse.getUrl()).isEqualTo(authenticationProviderUrl);
    }

    @Test
    void login_redirectsToRedirectUrl_IfTokenIsValid() {

        final String validToken = "a-valid-token";
        final String redirectUrl = "https://www.find-government-grants.service.gov.uk/";
        final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

        when(thirdPartyJwtService.isTokenValid(validToken)).thenReturn(true);

        final RedirectView methodeResponse = controllerUnderTest.login(validToken, redirectUrl, response);

        verify(response).addCookie(new Cookie("redirectUrl", redirectUrl));
        verify(thirdPartyJwtService).isTokenValid(validToken);
        assertThat(methodeResponse.getUrl()).isEqualTo(successfulRedirectUrl);
    }

    @Test
    void redirectOnLogin_RedirectsToProvidedLocation() {
        final String redirectUrl = "https://www.find-government-grants.service.gov.uk/";

        final RedirectView methodeResponse = controllerUnderTest.redirectOnLogin(redirectUrl);

        assertThat(methodeResponse.getUrl()).isEqualTo(redirectUrl);
    }

    @Test
    void validateUser_NullJwt() {
        final ResponseEntity<Boolean> response = controllerUnderTest.ValidateUser(null);
        assertThat(response.getBody()).isEqualTo(false);
    }

    @Test
    void validateUser_JwtNotValid() {
        final String invalidOrExpiredToken = "an-invalid-or-expired-token";

        //TODO change from thirdPartyJwtService to customJwtServiceImpl
        when(thirdPartyJwtService.isTokenValid(invalidOrExpiredToken)).thenReturn(false);

        final ResponseEntity<Boolean>  response = controllerUnderTest.ValidateUser(invalidOrExpiredToken);

        verify(thirdPartyJwtService).isTokenValid(invalidOrExpiredToken);
        assertThat(response.getBody()).isEqualTo(false);
    }

    @Test
    void validateUser_JwtValid() {
        final String validToken = "a-valid-token";

        //TODO change from thirdPartyJwtService to customJwtServiceImpl
        when(thirdPartyJwtService.isTokenValid(validToken)).thenReturn(true);

        final ResponseEntity<Boolean>  response = controllerUnderTest.ValidateUser(validToken);

        verify(thirdPartyJwtService).isTokenValid(validToken);
        assertThat(response.getBody()).isEqualTo(true);
    }
}