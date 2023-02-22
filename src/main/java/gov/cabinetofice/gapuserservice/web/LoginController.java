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
    private final ColaJwtServiceImpl thirdPartyJwtService;
    private final CustomJwtServiceImpl customJwtService;
    public static final String REDIRECT_URL_COOKIE = "redirectUrl";
    public static final String THIRD_PARTY_COOKIE_NAME = "find-grants-test";
    public static final String USER_SERVICE_COOKIE_NAME = "user-service-token";

    @GetMapping("/login")
    public RedirectView login(final @CookieValue(name = USER_SERVICE_COOKIE_NAME, required = false) String jwt,
                              final @CookieValue(name = THIRD_PARTY_COOKIE_NAME, required = false) String thirdPartyJwt,
                              final @RequestParam String redirectUrl,
                              final HttpServletResponse response) {
        // TODO set strict, domain, http only etc
        response.addCookie(new Cookie(REDIRECT_URL_COOKIE, redirectUrl));

        // Removes the prepended "s:"
        final String colaJwt = thirdPartyJwt != null ? thirdPartyJwt.substring(2) : null;

        final boolean isTokenValid = isTokenValid(jwt, () -> customJwtService.isTokenValid(jwt));
        final boolean isThirdPartyTokenValid = isTokenValid(colaJwt, () -> thirdPartyJwtService.isTokenValid(colaJwt));

        if (!isTokenValid) {
            if (!isThirdPartyTokenValid) {
                return new RedirectView(authenticationProvider.getUrl());
            }
            final DecodedJWT decodedJWT = thirdPartyJwtService.decodeJwt(colaJwt);
            final String generatedToken = customJwtService.generateToken(decodedJWT);
            // TODO set strict, domain, http only etc
            response.addCookie(new Cookie(USER_SERVICE_COOKIE_NAME, generatedToken));
        }

        return new RedirectView("/redirect-after-login");
    }

    @GetMapping("/redirect-after-login")
    public RedirectView redirectAfterLoggedIn(final @CookieValue(REDIRECT_URL_COOKIE) String redirectUrl) {
        return new RedirectView(redirectUrl);
    }

    @GetMapping("/redirect-after-third-party-login")
    //TODO make sure COLA's redirect URL points to here
    public RedirectView redirectAfterThirdPartyLogin(final @CookieValue(REDIRECT_URL_COOKIE) String redirectUrl) {
        return new RedirectView("/login?redirectUrl=" + redirectUrl);
    }

    private boolean isTokenValid(final String token, final Supplier<Boolean> isTokenValidFunc) {
        return token != null && isTokenValidFunc.get();
    }
}
