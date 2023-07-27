package gov.cabinetofice.gapuserservice.web;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import gov.cabinetofice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetofice.gapuserservice.config.FindAGrantConfigProperties;
import gov.cabinetofice.gapuserservice.dto.OneLoginUserInfoDto;
import gov.cabinetofice.gapuserservice.dto.PrivacyPolicyDto;
import gov.cabinetofice.gapuserservice.enums.LoginJourneyRedirect;
import gov.cabinetofice.gapuserservice.enums.LoginJourneyState;
import gov.cabinetofice.gapuserservice.exceptions.UnauthorizedException;
import gov.cabinetofice.gapuserservice.exceptions.UserNotFoundException;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.OneLoginService;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import gov.cabinetofice.gapuserservice.util.WebUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.WebUtils;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Controller
@RequestMapping("v2")
@ConditionalOnProperty(value = "feature.onelogin.enabled", havingValue = "true")
public class LoginControllerV2 {

    private final OneLoginService oneLoginService;
    private final CustomJwtServiceImpl customJwtService;
    private final ApplicationConfigProperties configProperties;

    private final FindAGrantConfigProperties findProperties;

    public static final String PRIVACY_POLICY_PAGE_VIEW = "privacy-policy";

    public static final String NOTICE_PAGE_VIEW = "notice-page";

    private static final String REDIRECT_URL_NAME = "redirectUrl";

    @Value("${jwt.cookie-name}")
    public String userServiceCookieName;

    @Value("${admin-base-url}")
    private String adminBaseUrl;

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
            final Cookie redirectUrlCookie = WebUtil.buildSecureCookie(REDIRECT_URL_NAME, redirectUrlParam.orElse(configProperties.getDefaultRedirectUrl()));
            response.addCookie(redirectUrlCookie);

            // TODO Decide on where to set and evaluate nonce and state
            return new RedirectView(NOTICE_PAGE_VIEW);
        }

        return new RedirectView(redirectUrlParam.orElse(configProperties.getDefaultRedirectUrl()));
    }

    @GetMapping("/redirect-after-login")
    public RedirectView redirectAfterLogin(final @CookieValue(name = REDIRECT_URL_NAME) String redirectUrlCookie,
                                           final HttpServletResponse response,
                                           final @RequestParam String code) {
        final OneLoginUserInfoDto userInfo = oneLoginService.getOneLoginUserInfoDto(code);
        final User user = oneLoginService.createOrGetUserFromInfo(userInfo);
        addCustomJwtCookie(response, userInfo);
        return new RedirectView(runStateMachine(redirectUrlCookie, user));
    }

    @GetMapping("/notice-page")
    public ModelAndView showNoticePage() {
        return new ModelAndView(NOTICE_PAGE_VIEW)
                .addObject("loginUrl", oneLoginService.getOneLoginAuthorizeUrl())
                .addObject("homePageUrl", findProperties.getUrl());
    }

    @GetMapping("/privacy-policy")
    public ModelAndView showPrivacyPolicyPage(final @ModelAttribute("privacyPolicy") PrivacyPolicyDto privacyPolicyDto) {
        return new ModelAndView(PRIVACY_POLICY_PAGE_VIEW)
                .addObject("homePageUrl", findProperties.getUrl());
    }

    @PostMapping("/privacy-policy")
    public ModelAndView showPrivacyPolicyPage(final @Valid @ModelAttribute("privacyPolicy") PrivacyPolicyDto privacyPolicyDto,
                                              final BindingResult result,
                                              final HttpServletRequest request,
                                              final @CookieValue(name = REDIRECT_URL_NAME) String redirectUrlCookie) {
        if (result.hasErrors()) return showPrivacyPolicyPage(privacyPolicyDto);
        final User user = getUserFromRequest(request).orElseThrow(() -> new UserNotFoundException("Privacy policy: Could not fetch user from jwt"));
        return new ModelAndView("redirect:" + runStateMachine(redirectUrlCookie, user));
    }

    private void addCustomJwtCookie(final HttpServletResponse response, final OneLoginUserInfoDto userInfo) {
        final Map<String, String> customJwtClaims = oneLoginService.generateCustomJwtClaims(userInfo);
        final String customServiceJwt = customJwtService.generateToken(customJwtClaims);
        final Cookie customJwt = WebUtil.buildSecureCookie(userServiceCookieName, customServiceJwt);
        response.addCookie(customJwt);
    }

    private Optional<User> getUserFromRequest(final HttpServletRequest request) {
        final Cookie customJWTCookie = WebUtils.getCookie(request, userServiceCookieName);
        if (customJWTCookie == null) throw new UnauthorizedException("No " + userServiceCookieName + " cookie found");
        final DecodedJWT decodedJWT = JWT.decode(customJWTCookie.getValue());
        return oneLoginService.getUserFromSub(decodedJWT.getSubject());
    }

    private String runStateMachine(final String redirectUrlCookie, final User user) {
        final LoginJourneyState newLoginJourneyState = user.getLoginJourneyState().nextState(oneLoginService, user);
        final LoginJourneyRedirect loginJourneyRedirect = newLoginJourneyState.getRedirectUrl(user.getRole().getName());
        return getRedirectUrlAsString(loginJourneyRedirect, redirectUrlCookie);
    }

    private String getRedirectUrlAsString(final LoginJourneyRedirect loginJourneyRedirect, final String redirectUrlCookie) {
        return switch (loginJourneyRedirect) {
            case SUPER_ADMIN_DASHBOARD -> adminBaseUrl + "?redirectUrl=/super-admin-dashboard";
            case ADMIN_DASHBOARD -> adminBaseUrl + "?redirectUrl=/dashboard";
            case PRIVACY_POLICY_PAGE -> PRIVACY_POLICY_PAGE_VIEW;
            case APPLICANT_APP -> redirectUrlCookie;
        };
    }
}
