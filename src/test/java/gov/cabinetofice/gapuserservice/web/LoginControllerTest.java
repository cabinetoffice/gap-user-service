package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.config.ThirdPartyAuthProviderProperties;
import gov.cabinetofice.gapuserservice.service.jwt.TestDecodedJwt;
import gov.cabinetofice.gapuserservice.service.jwt.impl.ColaJwtServiceImpl;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.view.RedirectView;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private ColaJwtServiceImpl thirdPartyJwtService;

    @Mock
    private CustomJwtServiceImpl customJwtService;

    private LoginController controllerUnderTest;

    private final String successfulRedirectUrl = "/redirect-after-login";
    private final String authenticationProviderUrl = "https://some-authentication-providder.com";

    @BeforeEach
    void setup() {
        final ThirdPartyAuthProviderProperties authenticationProvider = ThirdPartyAuthProviderProperties.builder()
                .url(authenticationProviderUrl)
                .build();

        controllerUnderTest = new LoginController(authenticationProvider, thirdPartyJwtService, customJwtService);
    }

    @Nested
    class Login {
        @Nested
        class RedirectsToCola {
            @Test
            void IfBothTokensAreNull() {
                final String redirectUrl = "https://www.find-government-grants.service.gov.uk/";
                final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

                final RedirectView methodeResponse = controllerUnderTest.login(null, null, redirectUrl, response);

                verify(response).addCookie(new Cookie("redirectUrl", redirectUrl));
                assertThat(methodeResponse.getUrl()).isEqualTo(authenticationProviderUrl);
            }

            @Test
            void IfCustomTokenIsNull_AndColaTokenIsInvalid() {
                final String colaToken = "an-invalid-token";
                final String redirectUrl = "https://www.find-government-grants.service.gov.uk/";
                final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

                when(thirdPartyJwtService.isTokenValid(colaToken))
                        .thenReturn(false);

                final RedirectView methodeResponse = controllerUnderTest.login(null, colaToken, redirectUrl, response);

                verify(response).addCookie(new Cookie("redirectUrl", redirectUrl));
                assertThat(methodeResponse.getUrl()).isEqualTo(authenticationProviderUrl);
            }

            @Test
            void IfCustomTokenIsInvalid_AndColaTokenIsInvalid() {
                final String colaToken = "an-invalid-token";
                final String customToken = "a-custom-invalid-token";
                final String redirectUrl = "https://www.find-government-grants.service.gov.uk/";
                final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

                when(thirdPartyJwtService.isTokenValid(colaToken))
                        .thenReturn(false);
                when(customJwtService.isTokenValid(customToken))
                        .thenReturn(false);

                final RedirectView methodeResponse = controllerUnderTest.login(customToken, colaToken, redirectUrl, response);

                verify(response).addCookie(new Cookie("redirectUrl", redirectUrl));
                assertThat(methodeResponse.getUrl()).isEqualTo(authenticationProviderUrl);
            }

            @Test
            void IfCustomTokenIsInvalid_AndColaTokenIsNull() {
                final String customToken = "a-custom-invalid-token";
                final String redirectUrl = "https://www.find-government-grants.service.gov.uk/";
                final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

                when(customJwtService.isTokenValid(customToken))
                        .thenReturn(false);

                final RedirectView methodeResponse = controllerUnderTest.login(customToken, null, redirectUrl, response);

                verify(response).addCookie(new Cookie("redirectUrl", redirectUrl));
                assertThat(methodeResponse.getUrl()).isEqualTo(authenticationProviderUrl);
            }
        }

        @Nested
        class CreateCustomJwt {
            @Test
            void IfCustomTokenIsNull_AndColaTokenIsValid() {
                final String colaToken = "a-valid-token";
                final TestDecodedJwt decodedColaToken = TestDecodedJwt.builder().build();
                final String customToken = "a-custom-jwt";

                final String redirectUrl = "https://www.find-government-grants.service.gov.uk/";
                final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

                when(thirdPartyJwtService.isTokenValid(colaToken))
                        .thenReturn(true);
                when(thirdPartyJwtService.decodeJwt(colaToken))
                        .thenReturn(decodedColaToken);
                when(customJwtService.generateToken(decodedColaToken))
                        .thenReturn(customToken);


                final RedirectView methodeResponse = controllerUnderTest.login(null, colaToken, redirectUrl, response);


                verify(response, atLeastOnce()).addCookie(new Cookie("redirectUrl", redirectUrl));
                verify(thirdPartyJwtService).decodeJwt(colaToken);
                verify(customJwtService).generateToken(decodedColaToken);
                verify(response, atLeastOnce()).addCookie(new Cookie(LoginController.USER_SERVICE_COOKIE_NAME, customToken));
                assertThat(methodeResponse.getUrl()).isEqualTo(successfulRedirectUrl);
            }

            @Test
            void IfCustomTokenIsInvalid_AndColaTokenIsValid() {
                final String colaToken = "a-valid-token";
                final TestDecodedJwt decodedColaToken = TestDecodedJwt.builder().build();
                final String customToken = "an-invalid-custom-jwt";

                final String redirectUrl = "https://www.find-government-grants.service.gov.uk/";
                final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

                when(thirdPartyJwtService.isTokenValid(colaToken))
                        .thenReturn(true);
                when(thirdPartyJwtService.decodeJwt(colaToken))
                        .thenReturn(decodedColaToken);
                when(customJwtService.generateToken(decodedColaToken))
                        .thenReturn(customToken);
                when(customJwtService.isTokenValid(customToken))
                        .thenReturn(false);


                final RedirectView methodeResponse = controllerUnderTest.login(customToken, colaToken, redirectUrl, response);


                verify(response, atLeastOnce()).addCookie(new Cookie("redirectUrl", redirectUrl));
                verify(thirdPartyJwtService).decodeJwt(colaToken);
                verify(customJwtService).generateToken(decodedColaToken);
                verify(response, atLeastOnce()).addCookie(new Cookie(LoginController.USER_SERVICE_COOKIE_NAME, customToken));
                assertThat(methodeResponse.getUrl()).isEqualTo(successfulRedirectUrl);
            }
        }

        @Nested
        class ReturnsValidToken {
            @Test
            void IfCustomTokenIsValid_AndColaTokenIsInvalid() {
                final String colaToken = "an-invalid-token";
                final String customToken = "a-custom-valid-token";
                final String redirectUrl = "https://www.find-government-grants.service.gov.uk/";
                final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

                when(thirdPartyJwtService.isTokenValid(colaToken))
                        .thenReturn(false);
                when(customJwtService.isTokenValid(customToken))
                        .thenReturn(true);

                final RedirectView methodeResponse = controllerUnderTest.login(customToken, colaToken, redirectUrl, response);

                verify(response).addCookie(new Cookie("redirectUrl", redirectUrl));
                verify(thirdPartyJwtService, times(0)).decodeJwt(any());
                verify(customJwtService, times(0)).generateToken(any());
                assertThat(methodeResponse.getUrl()).isEqualTo(successfulRedirectUrl);
            }

            @Test
            void IfCustomTokenIsValid_AndColaTokenIsNull() {
                final String customToken = "a-custom-valid-token";
                final String redirectUrl = "https://www.find-government-grants.service.gov.uk/";
                final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

                when(customJwtService.isTokenValid(customToken))
                        .thenReturn(true);

                final RedirectView methodeResponse = controllerUnderTest.login(customToken, null, redirectUrl, response);

                verify(response).addCookie(new Cookie("redirectUrl", redirectUrl));
                verify(thirdPartyJwtService, times(0)).decodeJwt(any());
                verify(customJwtService, times(0)).generateToken(any());
                assertThat(methodeResponse.getUrl()).isEqualTo(successfulRedirectUrl);
            }

            @Test
            void IfBothTokensAreValid() {
                final String colaToken = "a-valid-token";
                final String customToken = "a-custom-valid-token";
                final String redirectUrl = "https://www.find-government-grants.service.gov.uk/";
                final HttpServletResponse response = Mockito.spy(new MockHttpServletResponse());

                when(thirdPartyJwtService.isTokenValid(colaToken))
                        .thenReturn(true);
                when(customJwtService.isTokenValid(customToken))
                        .thenReturn(true);

                final RedirectView methodeResponse = controllerUnderTest.login(customToken, colaToken, redirectUrl, response);

                verify(response).addCookie(new Cookie("redirectUrl", redirectUrl));
                verify(thirdPartyJwtService, times(0)).decodeJwt(any());
                verify(customJwtService, times(0)).generateToken(any());
                assertThat(methodeResponse.getUrl()).isEqualTo(successfulRedirectUrl);
            }

        }
    }

    @Test
    void redirectOnLogin_RedirectsToProvidedLocation() {
        final String redirectUrl = "https://www.find-government-grants.service.gov.uk/";

        final RedirectView methodeResponse = controllerUnderTest.redirectOnLogin(redirectUrl);

        assertThat(methodeResponse.getUrl()).isEqualTo(redirectUrl);
    }
}