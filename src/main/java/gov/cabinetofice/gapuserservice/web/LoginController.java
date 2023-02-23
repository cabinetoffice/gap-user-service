package gov.cabinetofice.gapuserservice.web;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import gov.cabinetofice.gapuserservice.config.ThirdPartyAuthProviderProperties;
import gov.cabinetofice.gapuserservice.service.ThirdPartyJwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
public class LoginController {

    private final ThirdPartyAuthProviderProperties authenticationProvider;
    private final ThirdPartyJwtService colaJwtService;
    private static final String REDIRECT_URL_COOKIE = "redirectUrl";
    private static final String COLA_COOKIE_NAME = "find-grants-test";
    public static final String USER_SERVICE_COOKIE_NAME = "user-service-token";

    @GetMapping("/login")
    public RedirectView login(final @CookieValue(name = COLA_COOKIE_NAME, required = false) String jwt,
            final @RequestParam String redirectUrl,
            final HttpServletResponse response) {
        response.addCookie(new Cookie(REDIRECT_URL_COOKIE, redirectUrl));

        if (jwt == null)
            return redirectToThirdParty();

        // TODO currently validating against the third party. When we switch to using
        // custom JWTs, we'll want to validate against our own definition instead
        final boolean isJwtValid = colaJwtService.isTokenValid(jwt);
        if (!isJwtValid)
            return redirectToThirdParty();

        return new RedirectView("/redirect-after-login");
    }

    @GetMapping("/redirect-after-login")
    public RedirectView redirectOnLogin(@CookieValue(REDIRECT_URL_COOKIE) String redirectUrl) {
        // TODO we'll want to validate the third party JWT here once we use custom JWTs
        return new RedirectView(redirectUrl);
    }

    private RedirectView redirectToThirdParty() {
        return new RedirectView(authenticationProvider.getUrl()); // TODO make sure COLA's redirect URL points to the
                                                                  // domain the user service sits on
    }

    @GetMapping("/is-user-logged-in")
    public ResponseEntity<Boolean> ValidateUser(
            final @CookieValue(name = USER_SERVICE_COOKIE_NAME, required = false) String jwt) {
        // TODO change from colaJwtService to CustomJwtServiceImpl
        final boolean isJwtValid = jwt != null ? colaJwtService.isTokenValid(jwt) : false;

        return ResponseEntity.ok(isJwtValid);
    }
}
