package gov.cabinetofice.gapuserservice.web;

import com.auth0.jwk.JwkException;
import com.auth0.jwt.interfaces.DecodedJWT;
import gov.cabinetofice.gapuserservice.config.ThirdPartyAuthProviderProperties;
import gov.cabinetofice.gapuserservice.service.ThirdPartyJwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;

@RequiredArgsConstructor
@Controller
public class LoginController {

    private final ThirdPartyAuthProviderProperties authenticationProvider;
    private final ThirdPartyJwtService thirdPartyJwtService;

    @GetMapping("/login")
    public RedirectView login(final @CookieValue(name = "find-grants-test", required = false) String jwt,
                              final @RequestParam String redirectUrl,
                              final HttpServletResponse response)
            throws JwkException {
        response.addCookie(new Cookie("redirectUrl", redirectUrl));

        if (jwt == null) return redirectToThirdParty();

        // TODO currently validating against the third party. When we switch to using custom JWTs, we'll want to validate against our own definition instead
        final DecodedJWT decodedJwt = thirdPartyJwtService.decodedJwt(jwt);
        final boolean isJwtValid = thirdPartyJwtService.verifyToken(decodedJwt);
        if (!isJwtValid) return redirectToThirdParty();

        return new RedirectView("/redirect-after-login");
    }

    @GetMapping("/redirect-after-login")
    public void getToken(@CookieValue(name = "redirectUrl", required = false) String redirectUrl, HttpServletResponse response) throws IOException {
        // TODO probably want to validate the third party JWT here once we use custom JWTs
        response.sendRedirect(redirectUrl);
    }

    private RedirectView redirectToThirdParty() {
        return new RedirectView(authenticationProvider.getUrl()); //TODO make sure COLA's redirect URL points to the domain the user service sits on
    }
}
