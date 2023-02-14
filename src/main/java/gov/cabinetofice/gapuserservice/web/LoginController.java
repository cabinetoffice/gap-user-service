package gov.cabinetofice.gapuserservice.web;

import com.auth0.jwt.interfaces.DecodedJWT;
import gov.cabinetofice.gapuserservice.config.ThirdPartyAuthProviderProperties;
import gov.cabinetofice.gapuserservice.service.JwtService;
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

    private final JwtService jwtService;

    @GetMapping("/login")
    public RedirectView login(@CookieValue(name = "user-service-token", required = false) String userServiceJwt, @RequestParam String redirectUrl,  HttpServletResponse response) {
        final boolean isTokenValid = jwtService.verifyUserServiceToken(userServiceJwt);

        if (!isTokenValid) {
            response.addCookie(new Cookie("redirect-url", redirectUrl)); //TODO make sure COLA's redirect URL points to the domain the user service sits on
            return new RedirectView(authenticationProvider.getUrl());
        }

        // if token is valid, redirect to their original location
        return new RedirectView(redirectUrl);
    }

    @GetMapping("/redirect-after-login")
    public void getToken(@CookieValue(name = "redirect-url", required = false) String redirectUrl,
                         @CookieValue(name = "find-grants-test", required = false) String colaJwt,
                         HttpServletResponse response) throws IOException {
        // TODO verify COLA JWT, return either decoded JWT or POJO with useful details
        DecodedJWT decodedColaJwt = null;

        String userServiceJwt = jwtService.generateTokenFromCOLAToken(decodedColaJwt);
        response.addCookie(new Cookie("user-service-token", userServiceJwt));

        response.sendRedirect(redirectUrl);
    }
}
