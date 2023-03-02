package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetofice.gapuserservice.config.ThirdPartyAuthProviderProperties;
import gov.cabinetofice.gapuserservice.exceptions.TokenNotValidException;
import gov.cabinetofice.gapuserservice.service.JwtBlacklistService;
import gov.cabinetofice.gapuserservice.service.jwt.impl.ColaJwtServiceImpl;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import gov.cabinetofice.gapuserservice.util.WebUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final JwtBlacklistService jwtBlacklistService;
    public static final String REDIRECT_URL_COOKIE = "redirectUrl";
    public static final String USER_SERVICE_COOKIE_NAME = "user-service-token";

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @GetMapping("/login")
    public RedirectView login(final @CookieValue(name = USER_SERVICE_COOKIE_NAME, required = false) String jwt,
                              final @RequestParam Optional<String> redirectUrl,
                              final HttpServletResponse response) {
        final boolean isTokenValid = jwt != null && customJwtService.isTokenValid(jwt);
        if (!isTokenValid) {
            final Cookie redirectUrlCookie = WebUtil.buildCookie(
                    new Cookie(REDIRECT_URL_COOKIE, redirectUrl.orElse(configProperties.getDefaultRedirectUrl())),
                    Boolean.TRUE,
                    Boolean.TRUE
            );

            response.addCookie(redirectUrlCookie);

            return new RedirectView(authenticationProvider.getUrl());
        }

        return new RedirectView(redirectUrl.orElse(configProperties.getDefaultRedirectUrl()));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @GetMapping("/redirect-after-cola-login")
    public RedirectView redirectAfterColaLogin(final @CookieValue(name = REDIRECT_URL_COOKIE) Optional<String> redirectUrl,
                                               final HttpServletRequest request,
                                               final HttpServletResponse response) {
        final Cookie tokenCookie = WebUtils.getCookie(request, authenticationProvider.getTokenCookie());
        if (tokenCookie == null || !thirdPartyJwtService.isTokenValid(tokenCookie.getValue())) {
            throw new TokenNotValidException("invalid token");
        }

        final Cookie userTokenCookie = WebUtil.buildCookie(
                new Cookie(USER_SERVICE_COOKIE_NAME, customJwtService.generateToken()),
                Boolean.TRUE,
                Boolean.TRUE
        );

        response.addCookie(userTokenCookie);

        return new RedirectView(redirectUrl.orElse(configProperties.getDefaultRedirectUrl()));
    }

    @PostMapping("/logout")
    public RedirectView logout(
            final @CookieValue(name = USER_SERVICE_COOKIE_NAME) String jwt,
            final HttpServletResponse response) {

        jwtBlacklistService.addJwtToBlacklist(jwt);

        final Cookie userTokenCookie = new Cookie(USER_SERVICE_COOKIE_NAME, null);
        userTokenCookie.setSecure(true);
        userTokenCookie.setHttpOnly(true);
        userTokenCookie.setMaxAge(0);

        response.addCookie(userTokenCookie);

        final String colaLogout = authenticationProvider.getLogoutUrl();

        return new RedirectView(colaLogout);
    }

    @GetMapping("/refresh-token")
    public ResponseEntity<String> refreshToken(@CookieValue(USER_SERVICE_COOKIE_NAME) final String currentToken, final HttpServletResponse response) {

        jwtBlacklistService.addJwtToBlacklist(currentToken);

        final String newToken = customJwtService.generateToken();
        final Cookie userTokenCookie = WebUtil.buildCookie(
                new Cookie(USER_SERVICE_COOKIE_NAME, newToken),
                Boolean.TRUE,
                Boolean.TRUE
        );

        response.addCookie(userTokenCookie);

        return ResponseEntity.ok(newToken);
    }

    @GetMapping("/is-user-logged-in")
    public ResponseEntity<Boolean> validateUser(
            final @CookieValue(name = USER_SERVICE_COOKIE_NAME, required = false) String jwt) {

        final boolean isJwtValid = jwt != null && customJwtService.isTokenValid(jwt);
        return ResponseEntity.ok(isJwtValid);
    }


}