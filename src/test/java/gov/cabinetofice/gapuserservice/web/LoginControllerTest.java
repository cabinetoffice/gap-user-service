package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.config.ThirdPartyAuthProviderProperties;
import gov.cabinetofice.gapuserservice.service.TestDecodedJwt;
import gov.cabinetofice.gapuserservice.service.jwt.JwtService;
import gov.cabinetofice.gapuserservice.service.jwt.impl.ColaJwtServiceImpl;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import org.hibernate.id.factory.spi.CustomIdGeneratorCreationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.view.RedirectView;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    private ThirdPartyAuthProviderProperties authenticationProvider;
    @Mock
    private ColaJwtServiceImpl thirdPartyJwtService;

    @Mock
    private CustomJwtServiceImpl customJwtService;

    private LoginController controllerUnderTest;

    private final String successfulRedirectUrl = "/redirect-after-login";
    private final String authenticationProviderUrl = "https://some-authentication-providder.com";

    @BeforeEach
    void setup() {
        authenticationProvider = ThirdPartyAuthProviderProperties.builder()
                .url(authenticationProviderUrl)
                .build();

        controllerUnderTest = new LoginController(authenticationProvider, thirdPartyJwtService, customJwtService);
    }

    // both null  x
    // custom is null and cola is invalid  x
    // custom is null and cola is valid  x
    // custom is invalid and cola is invalid
    // custom is invalid and cola is valid
    // custom is valid and cola is invalid
    // custom is valid and cola is valid
    // custom is invalid and cola is null
    // custom is valid and cola is null

    @Test
    void login_redirectsToCola_IfBothTokensAreNull() {
        final String redirectUrl = "https://www.find-government-grants.service.gov.uk/";
        final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

        final RedirectView methodeResponse = controllerUnderTest.login(null, null, redirectUrl, response);

        verify(response).addCookie(new Cookie("redirectUrl", redirectUrl));
        assertThat(methodeResponse.getUrl()).isEqualTo(authenticationProviderUrl);
    }

    @Test
    void login_redirectsToCola_IfCustomTokenIsNull_AndColaTokenIsInvalid() {
        final String colaToken = "an-invalid-tokem";
        final String redirectUrl = "https://www.find-government-grants.service.gov.uk/";
        final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

        when(thirdPartyJwtService.isTokenValid(colaToken))
                .thenReturn(false);

        final RedirectView methodeResponse = controllerUnderTest.login(null, colaToken, redirectUrl, response);

        verify(response).addCookie(new Cookie("redirectUrl", redirectUrl));
        assertThat(methodeResponse.getUrl()).isEqualTo(authenticationProviderUrl);
    }

    @Test
    void login_ShouldCreateCustomJwt_IfCustomTokenIsNull_AndColaTokenIsValid() {
        final String colaToken = "a-valid-tokem";
        final TestDecodedJwt decodedColaToken = TestDecodedJwt.builder().build();
        final String customToken = "a-custom-jwt";

        final String redirectUrl = "https://www.find-government-grants.service.gov.uk/";
        final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

        when(thirdPartyJwtService.isTokenValid(colaToken))
                .thenReturn(true);
        when(thirdPartyJwtService.decodeJwt(colaToken))
                .thenReturn(decodedColaToken);
        when(customJwtService.generateTokenFromCOLAToken(decodedColaToken))
                .thenReturn(customToken);


        final RedirectView methodeResponse = controllerUnderTest.login(null, colaToken, redirectUrl, response);


        verify(response, atLeastOnce()).addCookie(new Cookie("redirectUrl", redirectUrl));
        verify(thirdPartyJwtService).decodeJwt(colaToken);
        verify(customJwtService).generateTokenFromCOLAToken(decodedColaToken);
        verify(response, atLeastOnce()).addCookie(new Cookie(LoginController.USER_SERVICE_COOKIE_NAME, customToken));
        assertThat(methodeResponse.getUrl()).isEqualTo("/redirect-after-login");
    }



//    @Test
//    void login_redirectsToAuthProvider_IfNoTokenProvided() {
//
//        final String redirectUrl = "https://www.find-government-grants.service.gov.uk/";
//        final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
//
//        final RedirectView methodeResponse = controllerUnderTest.login(null, redirectUrl, response);
//
//        verify(response).addCookie(new Cookie("redirectUrl", redirectUrl));
//        assertThat(methodeResponse.getUrl()).isEqualTo(authenticationProviderUrl);
//    }
//
//    @Test
//    void login_redirectsToAuthProvider_IfTokenIsNotValid() {
//
//        final String invalidOrExpiredToken = "an-invalid-or-expired-token";
//        final String redirectUrl = "https://www.find-government-grants.service.gov.uk/";
//        final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
//
//        when(thirdPartyJwtService.isTokenValid(invalidOrExpiredToken)).thenReturn(false);
//
//        final RedirectView methodeResponse = controllerUnderTest.login(invalidOrExpiredToken, redirectUrl, response);
//
//        verify(response).addCookie(new Cookie("redirectUrl", redirectUrl));
//        verify(thirdPartyJwtService).isTokenValid(invalidOrExpiredToken);
//        assertThat(methodeResponse.getUrl()).isEqualTo(authenticationProviderUrl);
//    }
//
//    @Test
//    void login_redirectsToRedirectUrl_IfTokenIsValid() {
//
//        final String validToken = "a-valid-token";
//        final String redirectUrl = "https://www.find-government-grants.service.gov.uk/";
//        final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
//
//        when(thirdPartyJwtService.isTokenValid(validToken)).thenReturn(true);
//
//        final RedirectView methodeResponse = controllerUnderTest.login(validToken, redirectUrl, response);
//
//        verify(response).addCookie(new Cookie("redirectUrl", redirectUrl));
//        verify(thirdPartyJwtService).isTokenValid(validToken);
//        assertThat(methodeResponse.getUrl()).isEqualTo(successfulRedirectUrl);
//    }
//
//    @Test
//    void redirectOnLogin_RedirectsToProvidedLocation() {
//        final String redirectUrl = "https://www.find-government-grants.service.gov.uk/";
//
//        final RedirectView methodeResponse = controllerUnderTest.redirectOnLogin(redirectUrl);
//
//        assertThat(methodeResponse.getUrl()).isEqualTo(redirectUrl);
//    }
}