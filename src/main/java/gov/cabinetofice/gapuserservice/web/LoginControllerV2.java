package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetofice.gapuserservice.dto.OneLoginUserInfoDto;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
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
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.WebUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static gov.cabinetofice.gapuserservice.web.LoginController.REDIRECT_URL_COOKIE;


@RequiredArgsConstructor
@Controller
@RequestMapping("v2")
@ConditionalOnProperty(value = "feature.onelogin.enabled", havingValue = "true")
public class LoginControllerV2 {

    private final OneLoginService oneLoginService;
    private final CustomJwtServiceImpl customJwtService;
    private final ApplicationConfigProperties configProperties;

    @Value("${jwt.cookie-name}")
    public String userServiceCookieName;

    @Value("${onelogin.base-url}")
    private String oneLoginBaseUrl;

    @Value("${admin-base-url}")
    private String adminBaseUrl;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
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

            // TODO check if this is the correct URL
            return new RedirectView(oneLoginBaseUrl);
        }

        return new RedirectView(redirectUrl.orElse(configProperties.getDefaultRedirectUrl()));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @GetMapping("/redirect-after-login")
    public RedirectView redirectAfterLogin(final @CookieValue(name = REDIRECT_URL_COOKIE) Optional<String> redirectUrl,
                                 final HttpServletResponse response,
                                 final @RequestParam String code) {
        final String jwt = oneLoginService.createOneLoginJwt();
        final String authToken = oneLoginService.getAuthToken(jwt, code);
        final OneLoginUserInfoDto userInfo = oneLoginService.getUserInfo(authToken);
        final boolean userExistsByEmail = oneLoginService.doesUserExistByEmail(userInfo.getEmail());
        final boolean userExistsBySub = oneLoginService.doesUserExistBySub(userInfo.getSub());

        final Cookie customJwtCookie = generateCustomJwtCookie(userInfo);
        response.addCookie(customJwtCookie);

        if (userExistsBySub) {
            final List<RoleEnum> userRoles = oneLoginService.getUsersRolesBySub(userInfo.getSub());
            return getRedirectView(userRoles, redirectUrl);
        }

        if (userExistsByEmail) {
            final List<RoleEnum> userRoles = oneLoginService.getUsersRolesByEmail(userInfo.getEmail());
            final boolean isApplicant = oneLoginService.isUserAnApplicant(userRoles);
            if (isApplicant) {
                // TODO GAP-1922: Create migration page with a yes/no option
                return new RedirectView("/should-migrate-data");
            } else {
                // TODO GAP-1932: Migrate cola user data to this admin
                oneLoginService.addSubToUser(userInfo.getSub(), userInfo.getEmail());
                return getRedirectView(userRoles, redirectUrl);
            }
        }

        final List<RoleEnum> userRoles = oneLoginService.createUser(userInfo.getSub(), userInfo.getEmail());
        return getRedirectView(userRoles, redirectUrl);
    }

    private Cookie generateCustomJwtCookie(final OneLoginUserInfoDto userInfo) {
        final Map<String, String> claims = new HashMap<>();
        claims.put("email", userInfo.getEmail());
        claims.put("sub", userInfo.getSub());

        return WebUtil.buildCookie(
                new Cookie(userServiceCookieName, customJwtService.generateToken(claims)),
                Boolean.TRUE,
                Boolean.TRUE,
                null
        );
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private RedirectView getRedirectView(final List<RoleEnum> userRoles, final Optional<String> redirectUrl) {
        if(oneLoginService.isUserASuperAdmin(userRoles)) return new RedirectView(adminBaseUrl + "/super-admin/dashboard");
        if(oneLoginService.isUserAnAdmin(userRoles)) return new RedirectView(adminBaseUrl + "/dashboard");
        return new RedirectView((redirectUrl.orElse(configProperties.getDefaultRedirectUrl())));
    }

}