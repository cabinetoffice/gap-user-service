package gov.cabinetoffice.gapuserservice.web;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import gov.cabinetoffice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetoffice.gapuserservice.config.FindAGrantConfigProperties;
import gov.cabinetoffice.gapuserservice.dto.*;
import gov.cabinetoffice.gapuserservice.enums.GetRedirectUrlArgs;
import gov.cabinetoffice.gapuserservice.enums.NextStateArgs;
import gov.cabinetoffice.gapuserservice.exceptions.UserNotFoundException;
import gov.cabinetoffice.gapuserservice.model.RoleEnum;
import gov.cabinetoffice.gapuserservice.model.User;
import gov.cabinetoffice.gapuserservice.service.OneLoginService;
import gov.cabinetoffice.gapuserservice.service.encryption.Sha512Service;
import gov.cabinetoffice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import gov.cabinetoffice.gapuserservice.service.user.OneLoginUserService;
import gov.cabinetoffice.gapuserservice.util.HelperUtils;
import gov.cabinetoffice.gapuserservice.util.LoggingUtils;
import gov.cabinetoffice.gapuserservice.util.WebUtil;
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

import java.net.MalformedURLException;
import java.util.*;
import java.util.stream.Collectors;

import static net.logstash.logback.argument.StructuredArguments.entries;

@RequiredArgsConstructor
@Controller
@RequestMapping("v2")
@ConditionalOnProperty(value = "feature.onelogin.enabled", havingValue = "true")
@Slf4j
public class LoginControllerV2 {
    private final OneLoginService oneLoginService;
    private final CustomJwtServiceImpl customJwtService;
    private final ApplicationConfigProperties configProperties;
    private final Sha512Service encryptionService;
    private final OneLoginUserService oneLoginUserService;
    private final FindAGrantConfigProperties findProperties;
    private final LoggingUtils loggingUtils;

    public static final String PRIVACY_POLICY_PAGE_VIEW = "privacy-policy";

    public static final String UPDATED_EMAIL_PAGE_VIEW = "updated-email";

    private static final String REDIRECT_URL_NAME = "redirectUrl";

    private static final String STATE_COOKIE = "state";

    @Value("${find-a-grant.url}")
    private String findAGrantBaseUrl;

    @Value("${jwt.cookie-name}")
    public String userServiceCookieName;

    @Value("${jwt.cookie-domain}")
    public String userServiceCookieDomain;

    @Value("${admin-base-url}")
    private String adminBaseUrl;

    @Value("${applicant-base-url}")
    private String applicantBaseUrl;

    @Value("${tech-support-dash-base-url}")
    private String techSupportAppBaseUrl;

    @Value("${feature.find-accounts.migration.enabled}")
    private String findAccountsMigrationEnabled;

    @Value("${onelogin.post-logout-redirect-uri}")
    private String postLogoutRedirectUri;

    @PostMapping("/validateSessionsRoles")
    public ResponseEntity<Boolean> validateSessionsRoles(
            @RequestBody final ValidateSessionsRolesRequestBodyDto requestBody) {
        oneLoginUserService.validateSessionsRoles(requestBody.emailAddress(), requestBody.roles());
        return ResponseEntity.ok(Boolean.TRUE);
    }

    @GetMapping("/login")
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public RedirectView login(
            final @RequestParam(name = REDIRECT_URL_NAME) Optional<String> redirectUrlParam,
            final HttpServletRequest request,
            final HttpServletResponse response) throws MalformedURLException {
        final Cookie customJWTCookie = WebUtils.getCookie(request, userServiceCookieName);

        log.info("customJWTCookie ::" + customJWTCookie);
        final boolean isTokenValid = customJWTCookie != null
                && customJWTCookie.getValue() != null
                && customJwtService.isTokenValid(customJWTCookie.getValue());

        log.info("isTokenValid ::" + isTokenValid);
        String redirectUrl = redirectUrlParam.orElse(null);

        if (redirectUrl != null) {
            WebUtil.validateRedirectUrl(redirectUrl, findAGrantBaseUrl);
        }

        if (!isTokenValid) {
            log.info("Token not valid ::");
            deleteJWTCookieIfPresent(request, response);

            final String nonce = oneLoginService.generateAndStoreNonce();
            String saltId = encryptionService.generateAndStoreSalt();
            final String encodedState = oneLoginService.generateAndStoreState(response, redirectUrl, saltId);
            final String state = encryptionService.getSHA512SecurePassword(encodedState, saltId);

            return new RedirectView(oneLoginService.getOneLoginAuthorizeUrl(state, nonce));
        }

        if (redirectUrl == null) {
            redirectUrl = configProperties.getDefaultRedirectUrl();
        }

        log.info("redirect URL ::" + redirectUrl);
        if (redirectUrl.endsWith("/404")) {
            final List<String> roles = getUserRolesFromJwtToken(customJWTCookie);

            redirectUrl = generate404UrlBasedOnHighestRole(roles);

            return new RedirectView(redirectUrl);
        }

        return new RedirectView(redirectUrl);
    }

    @GetMapping("/redirect-after-login")
    public RedirectView redirectAfterLogin(
            final @CookieValue(name = STATE_COOKIE) String stateCookie,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final @RequestParam String code,
            final @RequestParam String state) {

        log.info("In redirectAfterLogin...");

        // Redirect user if they are already logged in
        final Cookie customJWTCookie = WebUtils.getCookie(request, userServiceCookieName);
        final boolean isTokenValid = customJWTCookie != null
                && customJWTCookie.getValue() != null
                && customJwtService.isTokenValid(customJWTCookie.getValue());

        if (isTokenValid) {

            final StateCookieDto stateCookieDto = oneLoginService.decodeStateCookie(stateCookie);
            final String redirectUrl = stateCookieDto.getRedirectUrl();
            log.info("Token valid. Redirectig to URL :" + redirectUrl);
            return new RedirectView(redirectUrl);
        }

        final JSONObject tokenResponse = oneLoginService.getOneLoginUserTokenResponse(code);
        log.info(
                loggingUtils.getLogMessage("one login token response: ", 1),
                entries(tokenResponse.toMap()));
        final String idToken = tokenResponse.getString("id_token");
        final String authToken = tokenResponse.getString("access_token");

        IdTokenDto decodedIdToken = oneLoginService.decodeTokenId(idToken);

        if (!Objects.equals(this.configProperties.getProfile(), "LOCAL")) {
            oneLoginService.validateIdToken(decodedIdToken);
            log.info("decoded token validation success");
            oneLoginService.validateAuthTokenSignatureAndAlgorithm(authToken);
            log.info("validateAuthTokenSignatureAndAlgorithm success");
        }

        final StateCookieDto stateCookieDto = oneLoginService.decodeStateCookie(stateCookie);
        final String redirectUrl = stateCookieDto.getRedirectUrl();

        oneLoginService.verifyStateAndNonce(decodedIdToken.getNonce(), stateCookieDto, state);

        final OneLoginUserInfoDto userInfo = oneLoginService.getOneLoginUserInfoDto(authToken);

        if (!Objects.equals(this.configProperties.getProfile(), "LOCAL")) {
            oneLoginService.validateUserSub(decodedIdToken.getSub(), userInfo.getSub());
        }

        final User user = oneLoginUserService.createOrGetUserFromInfo(userInfo);
        log.info("User --" + user.getEmailAddress());

        final Cookie customJwtCookie = addCustomJwtCookie(response, userInfo, idToken, user.isAdmin());

        // recreate state cookie and set age to 0 to delete it. Avoids possible unwanted
        // redirection if state cookie persist
        deleteStateCookie(response);

        return new RedirectView(runStateMachine(redirectUrl, user, customJwtCookie.getValue(),
                user.hasAcceptedPrivacyPolicy(), userInfo));
    }

    @GetMapping("/updated-email")
    public ModelAndView updatedEmailPage(
            final HttpServletRequest request,
            final @CookieValue(name = STATE_COOKIE, required = false) Optional<String> stateCookie) {
        final Cookie customJWTCookie = HelperUtils.getCustomJwtCookieFromRequest(request, userServiceCookieName);
        final User user = getUserFromCookie(customJWTCookie)
                .orElseThrow(() -> new UserNotFoundException("Update email: Could not fetch user from jwt"));

        final String redirectUrlCookieValue = getRedirectUrlFromStateCookie(stateCookie);

        final String redirectUrl = runStateMachine(redirectUrlCookieValue,
                user,
                customJWTCookie.getValue(), true, null);

        return new ModelAndView(UPDATED_EMAIL_PAGE_VIEW).addObject("email", user.getEmailAddress())
                .addObject(REDIRECT_URL_NAME, redirectUrl).addObject("homePageUrl", findProperties.getUrl());
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
            final @CookieValue(name = STATE_COOKIE, required = false) Optional<String> stateCookie) {
        if (result.hasErrors())
            return submitToPrivacyPolicyPage(privacyPolicyDto);
        final Cookie customJWTCookie = HelperUtils.getCustomJwtCookieFromRequest(request, userServiceCookieName);
        final User user = getUserFromCookie(customJWTCookie)
                .orElseThrow(() -> new UserNotFoundException("Privacy policy: Could not fetch user from jwt"));

        final String redirectUrlCookieValue = getRedirectUrlFromStateCookie(stateCookie);

        return new ModelAndView(
                "redirect:" + runStateMachine(redirectUrlCookieValue, user,
                        customJWTCookie.getValue(), true, null));
    }

    @GetMapping("/logout")
    public RedirectView logout(final HttpServletRequest request, final HttpServletResponse response) {
        final Cookie customJWTCookie = WebUtils.getCookie(request, userServiceCookieName);
        if (customJWTCookie == null || customJWTCookie.getValue().isBlank()) {
            return new RedirectView(postLogoutRedirectUri);
        }

        return oneLoginService.logoutUser(customJWTCookie, response);
    }

    private Cookie addCustomJwtCookie(final HttpServletResponse response,
            final OneLoginUserInfoDto userInfo,
            final String idToken,
            final boolean isAdmin) {
        final Map<String, String> customJwtClaims = oneLoginService.generateCustomJwtClaims(userInfo, idToken);
        final String customServiceJwt = customJwtService.generateToken(customJwtClaims, isAdmin);
        final Cookie customJwt = WebUtil.buildSecureCookie(userServiceCookieName, customServiceJwt,
                userServiceCookieDomain);
        response.addCookie(customJwt);
        return customJwt;
    }

    private String getRedirectUrlFromStateCookie(Optional<String> stateCookie) {
        return stateCookie.isPresent() ? oneLoginService.decodeStateCookie(stateCookie.get()).getRedirectUrl()
                : configProperties.getDefaultRedirectUrl();
    }

    private Optional<User> getUserFromCookie(final Cookie customJWTCookie) {
        final DecodedJWT decodedJWT = JWT.decode(customJWTCookie.getValue());
        return oneLoginUserService.getUserFromSub(decodedJWT.getSubject());
    }

    private String runStateMachine(final String redirectUrlCookie, final User user, final String jwt,
            final boolean hasAcceptedPrivacyPolicy, final OneLoginUserInfoDto userInfo) {
        log.info(loggingUtils.getLogMessage("Running state machine", 5), redirectUrlCookie, user, jwt,
                hasAcceptedPrivacyPolicy, userInfo);

        String redirectUrl = user.getLoginJourneyState()
                .nextState(new NextStateArgs(oneLoginUserService, user, jwt, log, hasAcceptedPrivacyPolicy, userInfo,
                        findAccountsMigrationEnabled))
                .getLoginJourneyRedirect(user.getHighestRole().getName(), redirectUrlCookie)
                .getRedirectUrl(new GetRedirectUrlArgs(adminBaseUrl, applicantBaseUrl, techSupportAppBaseUrl,
                        redirectUrlCookie, user));
        log.info(loggingUtils.getLogMessage("Redirecting to: ", 1), redirectUrl);
        return redirectUrl;
    }

    private void deleteStateCookie(HttpServletResponse response) {
        final Cookie stateCookieReplacement = WebUtil.buildCookie(
                new Cookie(STATE_COOKIE, null),
                Boolean.TRUE,
                Boolean.TRUE,
                null);
        stateCookieReplacement.setMaxAge(0);
        response.addCookie(stateCookieReplacement);
    }

    private void deleteJWTCookieIfPresent(final HttpServletRequest request, final HttpServletResponse response) {
        final Cookie customJWTCookie = WebUtils.getCookie(request, userServiceCookieName);
        if (customJWTCookie == null)
            return;
        final Cookie nullCookie = WebUtil.buildSecureCookie(userServiceCookieName, "deleted");
        nullCookie.setMaxAge(0);
        response.addCookie(nullCookie);
    }

    private String generate404UrlBasedOnHighestRole(List<String> roles) {
        final String PAGE_404 = "/404";
        if (roles.contains(RoleEnum.SUPER_ADMIN.name()) || roles.contains(RoleEnum.ADMIN.name()))
            return adminBaseUrl + PAGE_404;
        if (roles.contains(RoleEnum.TECHNICAL_SUPPORT.name()))
            return techSupportAppBaseUrl + PAGE_404;
        if (roles.contains(RoleEnum.APPLICANT.name()))
            return applicantBaseUrl + PAGE_404;
        return PAGE_404;
    }

    private List<String> getUserRolesFromJwtToken(Cookie customJWTCookie) {
        final DecodedJWT decodedJwt = customJwtService.decodedJwt(customJWTCookie.getValue());
        final JwtPayload payload = customJwtService.decodeTheTokenPayloadInAReadableFormat(decodedJwt);
        return List.of(payload.getRoles()
                .replace("[", "")
                .replace("]", "")
                .replace(" ", "")
                .split(","));
    }
}
