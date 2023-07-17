package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetofice.gapuserservice.dto.OneLoginUserInfoDto;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.OneLoginService;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import gov.cabinetofice.gapuserservice.util.WebUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.WebUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static gov.cabinetofice.gapuserservice.web.LoginController.REDIRECT_URL_COOKIE;

@RequiredArgsConstructor
@Controller
@RequestMapping("v2")
@ConditionalOnProperty(value = "feature.onelogin.enabled", havingValue = "true")
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class LoginControllerV2 {

    private final OneLoginService oneLoginService;
    private final CustomJwtServiceImpl customJwtService;
    private final ApplicationConfigProperties configProperties;

    public static final String NOTICE_PAGE_VIEW = "notice-page";

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
            if (user.hasSub()) return getRedirectView(user, redirectUrl);
            if (user.isApplicant()) {
                // TODO GAP-1922: Create migration page with a yes/no option
                return new RedirectView("/should-migrate-data");
            } else {
                // TODO GAP-1932: Migrate cola user data to this admin
                oneLoginService.addSubToUser(userInfo.getSub(), user.getEmailAddress());
                return getRedirectView(user, redirectUrl);
            }
        }

        final User user = oneLoginService.createUser(userInfo.getSub(), userInfo.getEmailAddress());
        return getRedirectView(user, redirectUrl);
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

    private RedirectView getRedirectView(final User user, final Optional<String> redirectUrl) {
        if(user.isSuperAdmin()) return new RedirectView(adminBaseUrl + "/super-admin/dashboard");
        if(user.isAdmin()) return new RedirectView(adminBaseUrl);
        return new RedirectView((redirectUrl.orElse(configProperties.getDefaultRedirectUrl())));
    }

    @GetMapping("/notice-page")
    public ModelAndView showNoticePage() {
        return new ModelAndView(NOTICE_PAGE_VIEW)
                .addObject("loginUrl", oneLoginService.getOneLoginAuthorizeUrl());
    }

}
