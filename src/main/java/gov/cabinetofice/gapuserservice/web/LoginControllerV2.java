package gov.cabinetofice.gapuserservice.web;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import gov.cabinetofice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetofice.gapuserservice.config.FindAGrantConfigProperties;
import gov.cabinetofice.gapuserservice.dto.*;
import gov.cabinetofice.gapuserservice.enums.NextStateArgs;
import gov.cabinetofice.gapuserservice.exceptions.NonceExpiredException;
import gov.cabinetofice.gapuserservice.exceptions.UnauthorizedClientException;
import gov.cabinetofice.gapuserservice.exceptions.UserNotFoundException;
import gov.cabinetofice.gapuserservice.model.Nonce;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.repository.NonceRepository;
import gov.cabinetofice.gapuserservice.service.OneLoginService;
import gov.cabinetofice.gapuserservice.service.RoleService;
import gov.cabinetofice.gapuserservice.service.encryption.Sha512Service;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import gov.cabinetofice.gapuserservice.service.user.OneLoginUserService;
import gov.cabinetofice.gapuserservice.util.LoggingUtils;
import gov.cabinetofice.gapuserservice.util.WebUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.WebUtils;

import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static net.logstash.logback.argument.StructuredArguments.entries;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

import static gov.cabinetofice.gapuserservice.util.HelperUtils.getCustomJwtCookieFromRequest;

@RequiredArgsConstructor
@Controller
@RequestMapping("v2")
@ConditionalOnProperty(value = "feature.onelogin.enabled", havingValue = "true")
@Slf4j
public class LoginControllerV2 {
    private final OneLoginService oneLoginService;
    private final RoleService roleService;
    private final CustomJwtServiceImpl customJwtService;
    private final ApplicationConfigProperties configProperties;
    private final Sha512Service encryptionService;
    private final NonceRepository nonceRepository;
    private final OneLoginUserService oneLoginUserService;
    private final FindAGrantConfigProperties findProperties;
    private final LoggingUtils loggingUtils;

    public static final String PRIVACY_POLICY_PAGE_VIEW = "privacy-policy";

    public static final String UPDATED_EMAIL_PAGE_VIEW = "updated-email";

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

    @GetMapping("/validateSessionsRoles")
    public ResponseEntity<Boolean> validateSessionsRoles(@RequestParam("emailAddress") String emailAddress,
                                                         @RequestParam("roles") String roles){
        oneLoginUserService.validateSessionsRoles(emailAddress, roles);
        return ResponseEntity.ok(Boolean.TRUE);
    }

    @GetMapping("/login")
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public RedirectView login(
            final @RequestParam(name = REDIRECT_URL_NAME) Optional<String> redirectUrlParam,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        final Cookie customJWTCookie = WebUtils.getCookie(request, userServiceCookieName);
        final boolean isTokenValid = customJWTCookie != null
                && customJWTCookie.getValue() != null
                && customJwtService.isTokenValid(customJWTCookie.getValue());
        final String redirectUrl = redirectUrlParam.orElse(configProperties.getDefaultRedirectUrl());
        if (!isTokenValid) {
            final String nonce = oneLoginService.generateAndStoreNonce();
            String saltId = encryptionService.generateAndStoreSalt();
            final String encodedState = oneLoginService.generateAndStoreState(response, redirectUrl, saltId);
            final String state = encryptionService.getSHA512SecurePassword(encodedState, saltId);

            return new RedirectView(oneLoginService.getOneLoginAuthorizeUrl(state, nonce));
        }

        return new RedirectView(redirectUrl);
    }

    @GetMapping("/redirect-after-login")
    public RedirectView redirectAfterLogin(
            final @CookieValue(name = STATE_COOKIE) String stateCookie,
            final HttpServletResponse response,
            final @RequestParam String code,
            final @RequestParam String state) {
        final JSONObject tokenResponse = oneLoginService.getOneLoginUserTokenResponse(code);
        log.info(
                loggingUtils.getLogMessage("one login token response: ", 1),
                entries(tokenResponse.toMap()));
        final String idToken = tokenResponse.getString("id_token");
        final String authToken = tokenResponse.getString("access_token");

        IdTokenDto decodedIdToken = oneLoginService.decodeTokenId(idToken);

        if (!Objects.equals(this.configProperties.getProfile(), "LOCAL")) {
            oneLoginService.validateIdToken(decodedIdToken);
            oneLoginService.validateAuthTokenSignatureAndAlgorithm(authToken);
        }

        final StateCookieDto stateCookieDto = oneLoginService.decodeStateCookie(stateCookie);
        final String redirectUrl = stateCookieDto.getRedirectUrl();

        verifyStateAndNonce(decodedIdToken.getNonce(), stateCookieDto, state);

        final OneLoginUserInfoDto userInfo = oneLoginService.getOneLoginUserInfoDto(authToken);

        if (!Objects.equals(this.configProperties.getProfile(), "LOCAL")) {
            oneLoginService.validateUserSub(decodedIdToken.getSub(), userInfo.getSub());
        }

        final User user = oneLoginService.createOrGetUserFromInfo(userInfo);

        addCustomJwtCookie(response, userInfo, idToken);
        return new RedirectView(runStateMachine(redirectUrl, user, "jwt", user.hasAcceptedPrivacyPolicy(), userInfo));
    }

    @GetMapping("/updated-email")
    public ModelAndView updatedEmailPage(
            final HttpServletRequest request,
            final @CookieValue(name = REDIRECT_URL_NAME, required = false) Optional<String> redirectUrlCookie) {
        final Cookie customJWTCookie = getCustomJwtCookieFromRequest(request, userServiceCookieName);
        final User user = getUserFromCookie(customJWTCookie)
                .orElseThrow(() -> new UserNotFoundException("Update email: Could not fetch user from jwt"));
        final String redirectUrl = runStateMachine(redirectUrlCookie.orElse(configProperties.getDefaultRedirectUrl()),
                user,
                customJWTCookie.getValue(), true, null);
        return new ModelAndView(UPDATED_EMAIL_PAGE_VIEW).addObject("email", user.getEmailAddress())
                .addObject("redirectUrl", redirectUrl);
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
            final @CookieValue(name = REDIRECT_URL_NAME, required = false) Optional<String> redirectUrlCookie) {
        if (result.hasErrors())
            return submitToPrivacyPolicyPage(privacyPolicyDto);
        final Cookie customJWTCookie = getCustomJwtCookieFromRequest(request, userServiceCookieName);
        final User user = getUserFromCookie(customJWTCookie)
                .orElseThrow(() -> new UserNotFoundException("Privacy policy: Could not fetch user from jwt"));
        return new ModelAndView(
                "redirect:" + runStateMachine(redirectUrlCookie.orElse(configProperties.getDefaultRedirectUrl()), user,
                        customJWTCookie.getValue(), true, null));
    }

    @GetMapping("/logout")
    public RedirectView logout(final HttpServletRequest request, final HttpServletResponse response) {
        final Cookie customJWTCookie = WebUtils.getCookie(request, userServiceCookieName);
        if (customJWTCookie == null || customJWTCookie.getValue().isBlank()) {
            return new RedirectView(applicantBaseUrl);
        }

        return oneLoginService.logoutUser(customJWTCookie, response);
    }

    private void addCustomJwtCookie(final HttpServletResponse response, final OneLoginUserInfoDto userInfo,
            final String idToken) {
        final Map<String, String> customJwtClaims = oneLoginService.generateCustomJwtClaims(userInfo, idToken);
        final String customServiceJwt = customJwtService.generateToken(customJwtClaims);
        final Cookie customJwt = WebUtil.buildSecureCookie(userServiceCookieName, customServiceJwt);
        response.addCookie(customJwt);
    }

    private Optional<User> getUserFromCookie(final Cookie customJWTCookie) {
        final DecodedJWT decodedJWT = JWT.decode(customJWTCookie.getValue());
        return oneLoginService.getUserFromSub(decodedJWT.getSubject());
    }

    private void verifyStateAndNonce(final String nonce, final StateCookieDto stateCookieDto, final String state) {
        // Validate that state returned is the same as the one stored in the cookie
        final String saltId = stateCookieDto.getSaltId();
        final String encodedStateJson = oneLoginService.buildEncodedStateJson(
                stateCookieDto.getRedirectUrl(),
                stateCookieDto.getState(),
                saltId
        );
        final String hashedStateCookie = encryptionService.getSHA512SecurePassword(encodedStateJson, saltId);
        final boolean isStateVerified = state.equals(hashedStateCookie);
        // by only deleting the salt if the state matches we can ensure that an attacker can't arbitrarily delete salts
        // as we know they haven't changed the saltId
        if (isStateVerified) encryptionService.deleteSalt(saltId);

        // Validate that nonce is stored in the DB
        final Nonce storedNonce = oneLoginService.readAndDeleteNonce(nonce);
        final boolean isNonceVerified = nonce.equals(storedNonce.getNonceString());

        // Validate that nonce is less than 10 mins old
        final boolean isNonceExpired = oneLoginService.isNonceExpired(storedNonce);

        if (!isStateVerified || !isNonceVerified) {
            log.error(
                    loggingUtils.getLogMessage("/redirect-after-login encountered unauthorised user", 7),
                    keyValue("nonceVerified", isNonceVerified),
                    keyValue("stateVerified", isStateVerified),
                    keyValue("nonceFromToken", nonce),
                    keyValue("nonceFromDB", storedNonce.getNonceString()),
                    keyValue("stateFromResponse", state),
                    keyValue("hashedStateFromCookie", hashedStateCookie),
                    keyValue("stateFromCookie", encodedStateJson)
            );
            throw new UnauthorizedClientException("User authorization failed");
        } else if (isNonceExpired) {
            log.error(
                    loggingUtils.getLogMessage("/redirect-after-login encountered unauthorized user - nonce expired", 7),
                    keyValue("nonceFromToken", nonce),
                    keyValue("nonceFromDB", storedNonce.getNonceString()),
                    keyValue("nonceCreatedAt", storedNonce.getCreatedAt()),
                    keyValue("now", new Date()),
                    keyValue("stateFromResponse", state),
                    keyValue("hashedStateFromCookie", hashedStateCookie),
                    keyValue("stateFromCookie", encodedStateJson)
            );
            throw new NonceExpiredException("User authorization failed, please try again");
        }
    }

    private String runStateMachine(final String redirectUrlCookie, final User user, final String jwt,
            final boolean hasAcceptedPrivacyPolicy, final OneLoginUserInfoDto userInfo) {
        log.debug(loggingUtils.getLogMessage("Running state machine", 5), redirectUrlCookie, user, jwt,
                hasAcceptedPrivacyPolicy, userInfo);

        String redirectUrl = user.getLoginJourneyState()
                .nextState(new NextStateArgs(oneLoginService, user, jwt, log, hasAcceptedPrivacyPolicy, userInfo))
                .getLoginJourneyRedirect(user.getHighestRole().getName())
                .getRedirectUrl(adminBaseUrl, applicantBaseUrl, techSupportAppBaseUrl, redirectUrlCookie);
        log.debug(loggingUtils.getLogMessage("Redirecting to: ", 1), redirectUrl);
        return redirectUrl;
    }
}
