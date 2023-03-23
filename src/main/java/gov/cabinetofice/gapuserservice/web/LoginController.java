package gov.cabinetofice.gapuserservice.web;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nimbusds.jose.jwk.JWKSet;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.WebUtils;

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
                new Cookie(USER_SERVICE_COOKIE_NAME, customJwtService.generateToken(claims)),
                Boolean.TRUE,
                Boolean.TRUE,
                null
        );

        log.debug("new token from redirectAfterColaLogin method: " + userTokenCookie.getValue());

        response.addCookie(userTokenCookie);

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

    @GetMapping("/refresh-token")
    public RedirectView refreshToken(@CookieValue(USER_SERVICE_COOKIE_NAME) final String currentToken,
                                   final HttpServletResponse response,
                                   final @RequestParam String redirectUrl) {
        jwtBlacklistService.addJwtToBlacklist(currentToken);

        final DecodedJWT decodedJWT = JWT.decode(currentToken);

        Map<String, String> claims = new HashMap<>();
        for (Map.Entry<String, Claim> entry : decodedJWT.getClaims().entrySet()) {
            claims.put(entry.getKey(), entry.getValue().asString());
        }

        final String newToken = customJwtService.generateToken(claims);
        final Cookie userTokenCookie = WebUtil.buildCookie(
                new Cookie(USER_SERVICE_COOKIE_NAME, newToken),
                Boolean.TRUE,
                Boolean.TRUE,
                null
        );

        response.addCookie(userTokenCookie);

        return new RedirectView(redirectUrl);
    }

    @GetMapping("/is-user-logged-in")
    public ResponseEntity<Boolean> validateUser(
            final @CookieValue(name = USER_SERVICE_COOKIE_NAME, required = false) String jwt) {
        log.debug("verifying token: " + jwt);

        final boolean isJwtValid = jwt != null && customJwtService.isTokenValid(jwt);

        log.debug("is token valid: " + isJwtValid);

        return ResponseEntity.ok(isJwtValid);
    }


    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Map<String, Object>> test() {
        JWKSet jwkset = this.customJwtService.getPublicJWKSet();
        return ResponseEntity.ok(jwkset.toJSONObject(true));
    }


}