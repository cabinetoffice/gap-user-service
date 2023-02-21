package gov.cabinetofice.gapuserservice.web;

import com.auth0.jwt.interfaces.DecodedJWT;
import gov.cabinetofice.gapuserservice.config.ThirdPartyAuthProviderProperties;
import gov.cabinetofice.gapuserservice.service.jwt.impl.ColaJwtServiceImpl;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.util.function.Supplier;

@RequiredArgsConstructor
@Controller
public class LoginController {

    private final ThirdPartyAuthProviderProperties authenticationProvider;
    private final ColaJwtServiceImpl colaJwtService;
    private final CustomJwtServiceImpl customJwtService;
    public static final String REDIRECT_URL_COOKIE = "redirectUrl";
    public static final String COLA_COOKIE_NAME = "find-grants-test";
    public static final String USER_SERVICE_COOKIE_NAME = "user-service-token";

    @GetMapping("/login")
    public RedirectView login(final @CookieValue(name = USER_SERVICE_COOKIE_NAME, required = false) String jwt,
                              final @CookieValue(name = COLA_COOKIE_NAME, required = false) String colaJwt,
                              final @RequestParam String redirectUrl,
                              final HttpServletResponse response) {
        response.addCookie(new Cookie(REDIRECT_URL_COOKIE, redirectUrl));

        final boolean isTokenValid = isTokenValid(jwt, () -> customJwtService.isTokenValid(jwt));
        final boolean isColaTokenValid = isTokenValid(colaJwt, () -> colaJwtService.isTokenValid(colaJwt));

        if (!isTokenValid) {
            if (!isColaTokenValid) {
                return redirectToThirdPartyAuthProvider();
            }

            final DecodedJWT decodedColaJwt = colaJwtService.decodeJwt(colaJwt);
            final String generatedToken = customJwtService.generateTokenFromCOLAToken(decodedColaJwt);
            response.addCookie(new Cookie(USER_SERVICE_COOKIE_NAME, generatedToken));
        }

        return new RedirectView("/redirect-after-login");
    }

    private boolean isTokenValid(final String token, final Supplier<Boolean> isTokenValidFunc) {
        return token != null && isTokenValidFunc.get();
    }

    @GetMapping("/redirect-after-login")
    public RedirectView redirectOnLogin(@CookieValue(REDIRECT_URL_COOKIE) String redirectUrl) {
        // TODO we'll want to validate the third party JWT here once we use custom JWTs
        return new RedirectView(redirectUrl);
    }

    private RedirectView redirectToThirdPartyAuthProvider() {
        return new RedirectView(authenticationProvider.getUrl()); //TODO make sure COLA's redirect URL points to the domain the user service sits on
    }
}
