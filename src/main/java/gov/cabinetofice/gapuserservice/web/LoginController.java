package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetofice.gapuserservice.config.ThirdPartyAuthProviderProperties;
import gov.cabinetofice.gapuserservice.exceptions.TokenNotValidException;
import gov.cabinetofice.gapuserservice.service.jwt.impl.ColaJwtServiceImpl;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.WebUtils;

import java.util.Optional;

@RequiredArgsConstructor
@Controller
public class LoginController {

    private final ThirdPartyAuthProviderProperties authenticationProvider;
    private final ApplicationConfigProperties configProperties;
    private final ColaJwtServiceImpl thirdPartyJwtService;
    private final CustomJwtServiceImpl customJwtService;
    public static final String REDIRECT_URL_COOKIE = "redirectUrl";
    public static final String USER_SERVICE_COOKIE_NAME = "user-service-token";

    @GetMapping("/login")
    public RedirectView login(final @CookieValue(name = USER_SERVICE_COOKIE_NAME)String jwt,
                              final @RequestParam Optional<String> redirectUrl,
                              final HttpServletResponse response) {
        final boolean isTokenValid = jwt != null && customJwtService.isTokenValid(jwt);
        if (!isTokenValid) {
            response.addCookie(new Cookie(REDIRECT_URL_COOKIE, redirectUrl.orElse(configProperties.getDefaultRedirectUrl())));
            return new RedirectView(authenticationProvider.getUrl());
        }

        return new RedirectView(redirectUrl.orElse(configProperties.getDefaultRedirectUrl()));
    }

    @GetMapping("/redirect-after-cola-login")
    public RedirectView redirectAfterColaLogin(final @CookieValue(value = REDIRECT_URL_COOKIE, required = false) Optional<String> redirectUrl,
                                               final HttpServletRequest request,
                                               final HttpServletResponse response) {
        final Cookie tokenCookie = WebUtils.getCookie(request, authenticationProvider.getTokenCookie());
        if (tokenCookie == null || !thirdPartyJwtService.isTokenValid(tokenCookie.getValue())) {
            throw new TokenNotValidException("invalid token");
        }

        response.addCookie(new Cookie(USER_SERVICE_COOKIE_NAME, customJwtService.generateToken()));
        return new RedirectView(redirectUrl
                .orElse(configProperties.getDefaultRedirectUrl())
        );
    }
}
