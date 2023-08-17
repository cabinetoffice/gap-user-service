package gov.cabinetofice.gapuserservice.web;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import gov.cabinetofice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetofice.gapuserservice.config.FindAGrantConfigProperties;
import gov.cabinetofice.gapuserservice.dto.*;
import gov.cabinetofice.gapuserservice.exceptions.UnauthorizedException;
import gov.cabinetofice.gapuserservice.exceptions.UserNotFoundException;
import gov.cabinetofice.gapuserservice.model.Nonce;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.repository.NonceRepository;
import gov.cabinetofice.gapuserservice.service.OneLoginService;
import gov.cabinetofice.gapuserservice.service.encryption.Sha512Service;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import gov.cabinetofice.gapuserservice.util.WebUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.WebUtils;

import java.util.*;

@RequiredArgsConstructor
@Controller
@RequestMapping("v2")
@ConditionalOnProperty(value = "feature.onelogin.enabled", havingValue = "true")
@Log4j2
@Getter
public class LoginControllerV2 {

    private final OneLoginService oneLoginService;
    private final CustomJwtServiceImpl customJwtService;
    private final ApplicationConfigProperties configProperties;
    @Autowired
    private final Sha512Service encryptionService;
    private final NonceRepository nonceRepository;

    private final FindAGrantConfigProperties findProperties;

    public static final String PRIVACY_POLICY_PAGE_VIEW = "privacy-policy";

    private static final String REDIRECT_URL_NAME = "redirectUrl";

    private final String STATE_COOKIE = "state";

    @Value("${jwt.cookie-name}")
    public String userServiceCookieName;

    @Value("${admin-base-url}")
    private String adminBaseUrl;

    @Value("${applicant-base-url}")
    private String applicantBaseUrl;

    @Value("${tech-support-dash-base-url}")
    private String techSupportAppBaseUrl;

    @Value("${feature.onelogin.migration.enabled}")
    public String migrationEnabled;

    @GetMapping("/login")
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public RedirectView login(final @RequestParam(name = REDIRECT_URL_NAME) Optional<String> redirectUrlParam,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        final Cookie customJWTCookie = WebUtils.getCookie(request, userServiceCookieName);
        final boolean isTokenValid = customJWTCookie != null
                && customJWTCookie.getValue() != null
                && customJwtService.isTokenValid(customJWTCookie.getValue());

        if (!isTokenValid) {
            final String state = encryptionService.getSHA512SecurePassword(
                    oneLoginService.generateAndStoreState(response, redirectUrlParam.orElse(configProperties.getDefaultRedirectUrl()))
            );
            final String nonce = oneLoginService.generateAndStoreNonce();

            return new RedirectView(oneLoginService.getOneLoginAuthorizeUrl(state, nonce));
        }

        return new RedirectView(redirectUrlParam.orElse(configProperties.getDefaultRedirectUrl()));
    }

    @GetMapping("/redirect-after-login")
    public RedirectView redirectAfterLogin(
            final @CookieValue(name = STATE_COOKIE) String stateCookie,
            final HttpServletResponse response,
            final @RequestParam String code,
            final @RequestParam String state
    ) {
        final JSONObject tokenResponse = oneLoginService.getOneLoginUserTokenResponse(code);
        IdTokenDto decodedIdToken = oneLoginService.getDecodedIdToken(tokenResponse);
        final String tokenNonce = decodedIdToken.getNonce();

        final StateCookieDto stateCookieDto = oneLoginService.decodeStateCookie(stateCookie);
        final String redirectUrl = stateCookieDto.getRedirectUrl();
        final String cookieState = stateCookieDto.getState();
        final String encodedStateJsonString = oneLoginService.buildEncodedStateJson(redirectUrl, cookieState);
        final String hashedStateCookie = encryptionService.getSHA512SecurePassword(encodedStateJsonString);

        final Nonce storedNonce = oneLoginService.readAndDeleteNonce(tokenNonce);
        final Boolean nonceExpired = oneLoginService.isNonceExpired(storedNonce);

        final String nonceString = storedNonce.getNonceString();
        final Boolean stateAndNonceVerified = tokenNonce.equals(nonceString) && !nonceExpired && state.equals(hashedStateCookie);

        if (stateAndNonceVerified) {
            final String authToken = tokenResponse.getString("access_token");
            final OneLoginUserInfoDto userInfo = oneLoginService.getOneLoginUserInfoDto(authToken);
            final User user = oneLoginService.createOrGetUserFromInfo(userInfo);
            addCustomJwtCookie(response, userInfo);
            return new RedirectView(user.getLoginJourneyState()
                    .getLoginJourneyRedirect(user.getHighestRole().getName())
                    .getRedirectUrl(adminBaseUrl, applicantBaseUrl, techSupportAppBaseUrl, redirectUrl));
        } else {
            log.warn("/redirect-after-login unauthorized user; nonce expired: {}, nonce matching: {} state matching: {}", nonceExpired, tokenNonce.equals(nonceString), state.equals(hashedStateCookie));
            throw new AccessDeniedException("User authorization failed");
        }
    }

    @GetMapping("/privacy-policy")
    public ModelAndView submitToPrivacyPolicyPage(
            final @ModelAttribute("privacyPolicy") PrivacyPolicyDto privacyPolicyDto) {
        return new ModelAndView(PRIVACY_POLICY_PAGE_VIEW)
                .addObject("homePageUrl", findProperties.getUrl());
    }

    @PostMapping("/privacy-policy")
    public ModelAndView submitToPrivacyPolicyPage(
            final @Valid @ModelAttribute("privacyPolicy") PrivacyPolicyDto privacyPolicyDto,
            final BindingResult result,
            final HttpServletRequest request,
            final @CookieValue(name = REDIRECT_URL_NAME) String redirectUrlCookie) {
        if (result.hasErrors())
            return submitToPrivacyPolicyPage(privacyPolicyDto);
        final Cookie customJWTCookie = getCustomJwtCookieFromRequest(request);
        final User user = getUserFromCookie(customJWTCookie)
                .orElseThrow(() -> new UserNotFoundException("Privacy policy: Could not fetch user from jwt"));
        return new ModelAndView("redirect:" + runStateMachine(redirectUrlCookie, user, customJWTCookie.getValue()));
    }

    private void addCustomJwtCookie(final HttpServletResponse response, final OneLoginUserInfoDto userInfo) {
        final Map<String, String> customJwtClaims = oneLoginService.generateCustomJwtClaims(userInfo);
        final String customServiceJwt = customJwtService.generateToken(customJwtClaims);
        final Cookie customJwt = WebUtil.buildSecureCookie(userServiceCookieName, customServiceJwt);
        response.addCookie(customJwt);
    }

    private Optional<User> getUserFromCookie(final Cookie customJWTCookie) {
        final DecodedJWT decodedJWT = JWT.decode(customJWTCookie.getValue());
        return oneLoginService.getUserFromSub(decodedJWT.getSubject());
    }

    private Cookie getCustomJwtCookieFromRequest(final HttpServletRequest request) {
        final Cookie customJWTCookie = WebUtils.getCookie(request, userServiceCookieName);
        if (customJWTCookie == null)
            throw new UnauthorizedException(userServiceCookieName + " cookie not found");
        return customJWTCookie;
    }

    private String runStateMachine(final String redirectUrlCookie, final User user, final String jwt) {
        return user.getLoginJourneyState()
                .nextState(oneLoginService, user, jwt, log)
                .getLoginJourneyRedirect(user.getHighestRole().getName())
                .getRedirectUrl(adminBaseUrl, applicantBaseUrl, techSupportAppBaseUrl, redirectUrlCookie);
    }
}
