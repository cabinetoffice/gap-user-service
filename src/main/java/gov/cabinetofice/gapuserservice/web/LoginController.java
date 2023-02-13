package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.config.ThirdPartyAuthProviderProperties;
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

    @GetMapping("/login")
    public RedirectView login(@CookieValue(name = "find-grants-test", required = false) String jwt, @RequestParam String redirectUrl,  HttpServletResponse response) {
        final boolean tokenExists = jwt != null;
        final boolean tokenIsValid = true; // TODO implement code to verify JWT

        response.addCookie(new Cookie("redirectUrl", redirectUrl)); //TODO make sure COLA's redirect URL points to the domain the user service sits on
        if (!tokenExists || !tokenIsValid) {
            return new RedirectView(authenticationProvider.getUrl());
        }

        return new RedirectView("/redirect-after-login");
    }

    @GetMapping("/redirect-after-login")
    public void getToken(@CookieValue(name = "redirectUrl", required = false) String redirectUrl, HttpServletResponse response) throws IOException {
        response.sendRedirect(redirectUrl);
    }
}
