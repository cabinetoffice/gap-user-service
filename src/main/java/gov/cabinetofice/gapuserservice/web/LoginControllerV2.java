package gov.cabinetofice.gapuserservice.web;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import gov.cabinetofice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetofice.gapuserservice.config.FindAGrantConfigProperties;
import gov.cabinetofice.gapuserservice.dto.OneLoginUserInfoDto;
import gov.cabinetofice.gapuserservice.dto.PrivacyPolicyDto;
import gov.cabinetofice.gapuserservice.model.Role;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Controller
@RequestMapping("v2")
@ConditionalOnProperty(value = "feature.onelogin.enabled", havingValue = "true")
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class LoginControllerV2 {

    private final OneLoginService oneLoginService;
    private final CustomJwtServiceImpl customJwtService;
    private final ApplicationConfigProperties configProperties;

    private final FindAGrantConfigProperties findProperties;

    public static final String PRIVACY_POLICY_PAGE_VIEW = "privacy-policy";

    public static final String NOTICE_PAGE_VIEW = "notice-page";

    private static final String REDIRECT_URL_COOKIE = "redirectUrl";

    @Value("${jwt.cookie-name}")
    public String userServiceCookieName;

    @Value("${onelogin.base-url}")
    private String oneLoginBaseUrl;

    @Value("${admin-base-url}")
    private String adminBaseUrl;

    @GetMapping("/login")
    public RedirectView login(final @RequestParam Optional<String> redirectUrl,
                              final HttpServletRequest request,
                              final HttpServletResponse response) {
        final Cookie customJWTCookie = WebUtils.getCookie(request, userServiceCookieName);

        final boolean isTokenValid = customJWTCookie != null
                && customJWTCookie.getValue() != null
                && customJwtService.isTokenValid(customJWTCookie.getValue());
        if (!isTokenValid) {
            final Cookie redirectUrlCookie = WebUtil.buildCookie(
                    new Cookie(REDIRECT_URL_COOKIE, redirectUrl.orElse(configProperties.getDefaultRedirectUrl())),
                    Boolean.TRUE,
                    Boolean.TRUE,
                    null
            );

            response.addCookie(redirectUrlCookie);

            // TODO : Decide on where to set and evaluate nonce and state
            return new RedirectView(NOTICE_PAGE_VIEW);
        }

        return new RedirectView(redirectUrl.orElse(configProperties.getDefaultRedirectUrl()));
    }

    @GetMapping("/redirect-after-login")
    public RedirectView redirectAfterLogin(final @CookieValue(name = REDIRECT_URL_COOKIE) Optional<String> redirectUrl,
                                 final HttpServletResponse response,
                                 final @RequestParam String code) {
        final String jwt = oneLoginService.createOneLoginJwt();
        final String authToken = oneLoginService.getAuthToken(jwt, code);
        final OneLoginUserInfoDto userInfo = oneLoginService.getUserInfo(authToken);
        final Optional<User> userOptional = oneLoginService.getUser(userInfo.getEmailAddress(), userInfo.getSub());

        final Cookie customJwt = generateCustomJwtCookie(userInfo, userOptional);
        response.addCookie(customJwt);

        if (userOptional.isPresent()) {
            final User user = userOptional.get();
            if (user.hasSub()) return getRedirectView(user, response, redirectUrl);
            if (!user.isAdmin()) {
                // TODO GAP-1922: Create migration page with a yes/no option
                return new RedirectView("/should-migrate-data");
            } else {
                // TODO GAP-1932: Migrate cola user data to this admin
                oneLoginService.addSubToUser(userInfo.getSub(), user.getEmailAddress());
                return getRedirectView(user, response, redirectUrl);
            }
        }

        final User user = oneLoginService.createUser(userInfo.getSub(), userInfo.getEmailAddress());
        return getRedirectView(user, response, redirectUrl);
    }

    private Cookie generateCustomJwtCookie(final OneLoginUserInfoDto userInfo, final Optional<User> userOptional) {
        final Map<String, String> claims = new HashMap<>();
        claims.put("email", userInfo.getEmailAddress());
        claims.put("sub", userInfo.getSub());

        if(userOptional.isPresent()) {
            final User user = userOptional.get();
            claims.put("roles", user.getRoles().stream().map(Role::getName).toList().toString());
            if (user.hasDepartment()) {
                claims.put("department", user.getDepartment().getName());
            }
        } else {
            claims.put("roles", oneLoginService.getNewUserRoles().toString());
        }

        return WebUtil.buildCookie(
                new Cookie(userServiceCookieName, customJwtService.generateToken(claims)),
                Boolean.TRUE,
                Boolean.TRUE,
                null
        );
    }

    private Cookie makeRedirectCookie(String redirectUrl) {
        return WebUtil.buildCookie(
                new Cookie(REDIRECT_URL_COOKIE, redirectUrl),
                Boolean.TRUE,
                Boolean.TRUE,
                null
        );
    }

    private RedirectView getRedirectView(final User user, final HttpServletResponse response, final @CookieValue(name = REDIRECT_URL_COOKIE) Optional<String> redirectUrlCookie) {
        if (user.hasAcceptedPrivacyPolicy()) {
            if (user.isSuperAdmin()) return new RedirectView(adminBaseUrl + "?redirectUrl=/super-admin-dashboard");
            if (user.isAdmin()) return new RedirectView(adminBaseUrl + "?redirectUrl=/dashboard");
            return new RedirectView(redirectUrlCookie.orElse(configProperties.getDefaultRedirectUrl()));
        } else {
            if (user.isSuperAdmin()) response.addCookie(makeRedirectCookie(adminBaseUrl + "?redirectUrl=/super-admin-dashboard"));
            else if (user.isAdmin()) response.addCookie(makeRedirectCookie(adminBaseUrl + "?redirectUrl=/dashboard"));
            return new RedirectView(PRIVACY_POLICY_PAGE_VIEW);
        }
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
    public ModelAndView showPrivacyPolicyPage(final @Valid @ModelAttribute("privacyPolicy") PrivacyPolicyDto privacyPolicyDto, final BindingResult result, final HttpServletRequest request, final @CookieValue(name = REDIRECT_URL_COOKIE) Optional<String> redirectUrl) {
        final Cookie customJWTCookie = WebUtils.getCookie(request, userServiceCookieName);
        DecodedJWT jwt = JWT.decode(customJWTCookie.getValue());

        if (result.hasErrors()) {
            return new ModelAndView(PRIVACY_POLICY_PAGE_VIEW);
        }

        oneLoginService.setPrivacyPolicy(jwt.getSubject());
        return new ModelAndView("redirect:"+redirectUrl.orElse(configProperties.getDefaultRedirectUrl()));
    }

}
