package gov.cabinetoffice.gapuserservice.web;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import gov.cabinetoffice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetoffice.gapuserservice.config.ThirdPartyAuthProviderProperties;
import gov.cabinetoffice.gapuserservice.exceptions.TokenNotValidException;
import gov.cabinetoffice.gapuserservice.exceptions.UserNotFoundException;
import gov.cabinetoffice.gapuserservice.model.User;
import gov.cabinetoffice.gapuserservice.service.JwtBlacklistService;
import gov.cabinetoffice.gapuserservice.service.jwt.impl.ColaJwtServiceImpl;
import gov.cabinetoffice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import gov.cabinetoffice.gapuserservice.util.WebUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.WebUtils;
import org.springframework.beans.factory.annotation.Value;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
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

    @Value("${jwt.cookie-domain}")
    public String userServiceCookieDomain;

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
                    Boolean.TRUE,
                    null
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

        final String trimmedToken = URLDecoder.decode(tokenCookie.getValue(), StandardCharsets.UTF_8).substring(2);
        final DecodedJWT decodedJWT = thirdPartyJwtService.decodeJwt(trimmedToken);

        Map<String, String> claims = new HashMap<>();
        for (Map.Entry<String, Claim> entry : decodedJWT.getClaims().entrySet()) {
            claims.put(entry.getKey(), entry.getValue().asString());
        }
        final Cookie userTokenCookie = WebUtil.buildCookie(
                new Cookie(USER_SERVICE_COOKIE_NAME, customJwtService.generateToken(claims, false)),
                Boolean.TRUE,
                Boolean.TRUE,
                null
        );

        log.debug("new token from redirectAfterColaLogin method: " + userTokenCookie.getValue());

        final String authenticationCookieDomain = Objects.equals(this.configProperties.getProfile(), "LOCAL") ? "localhost" : "cabinetoffice.gov.uk";
        final Cookie thirdPartyAuthToken = WebUtil.buildCookie(
                new Cookie(authenticationProvider.getTokenCookie(), null),
                Boolean.TRUE,
                Boolean.TRUE,
                authenticationCookieDomain
        );
        thirdPartyAuthToken.setMaxAge(0);

        response.addCookie(userTokenCookie);
        response.addCookie(thirdPartyAuthToken);

        return new RedirectView(redirectUrl.orElse(configProperties.getDefaultRedirectUrl()));
    }

    @GetMapping("/logout")
    public RedirectView logout(
            final @CookieValue(name = USER_SERVICE_COOKIE_NAME, required = false) String jwt,
            final HttpServletResponse response) {

        if (jwt != null) {
            jwtBlacklistService.addJwtToBlacklist(jwt);
        }

        final Cookie userTokenCookie = WebUtil.buildCookie(
                new Cookie(USER_SERVICE_COOKIE_NAME, null),
                Boolean.TRUE,
                Boolean.TRUE,
                null
        );
        userTokenCookie.setMaxAge(0);

        final String authenticationCookieDomain = Objects.equals(this.configProperties.getProfile(), "LOCAL") ? "localhost" : "cabinetoffice.gov.uk";
        log.info("auth token domain: " + authenticationCookieDomain);

        final Cookie thirdPartyAuthToken = WebUtil.buildCookie(
                new Cookie(authenticationProvider.getTokenCookie(), null),
                Boolean.TRUE,
                Boolean.TRUE,
                authenticationCookieDomain
        );
        thirdPartyAuthToken.setMaxAge(0);

        response.addCookie(userTokenCookie);
        response.addCookie(thirdPartyAuthToken);

        return new RedirectView(authenticationProvider.getLogoutUrl());
    }

    @RequestMapping(value = "/refresh-token", method = { RequestMethod.GET, RequestMethod.POST })
    public RedirectView refreshToken(@CookieValue(USER_SERVICE_COOKIE_NAME) final String currentToken,
                                     final HttpServletRequest request,
                                     final HttpServletResponse response,
                                     final @RequestParam String redirectUrl) {
        jwtBlacklistService.addJwtToBlacklist(currentToken);

        final DecodedJWT decodedJWT = JWT.decode(currentToken);

        Map<String, String> claims = new HashMap<>();
        for (Map.Entry<String, Claim> entry : decodedJWT.getClaims().entrySet()) {
            claims.put(entry.getKey(), entry.getValue().asString());
        }
        final User user = customJwtService.getUserFromJwt(request).orElseThrow(() -> new UserNotFoundException("Refresh-token: User not found " + currentToken));
        final String newToken = customJwtService.generateToken(claims, user.isAdmin());
        final Cookie userTokenCookie = WebUtil.buildCookie(
                new Cookie(USER_SERVICE_COOKIE_NAME, newToken),
                Boolean.TRUE,
                Boolean.TRUE,
                userServiceCookieDomain
        );

        response.addCookie(userTokenCookie);

        final RedirectView redirectView = new RedirectView(redirectUrl);
        redirectView.setStatusCode(HttpStatusCode.valueOf(307));
        return redirectView;
    }

    @GetMapping("/is-user-logged-in")
    public ResponseEntity<Boolean> validateUser(
            final @CookieValue(name = USER_SERVICE_COOKIE_NAME, required = false) String jwt) {
        log.debug("verifying token: " + jwt);

        final boolean isJwtValid = jwt != null && customJwtService.isTokenValid(jwt);

        log.debug("is token valid: " + isJwtValid);

        return ResponseEntity.ok(isJwtValid);
    }

}