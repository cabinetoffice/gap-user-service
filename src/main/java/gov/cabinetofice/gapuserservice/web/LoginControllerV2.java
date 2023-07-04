package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.config.ApplicationConfigProperties;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.service.OneLoginService;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import gov.cabinetofice.gapuserservice.util.WebUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
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
    public RedirectView redirect(final @CookieValue(name = REDIRECT_URL_COOKIE) Optional<String> redirectUrl,
                                 final HttpServletRequest request,
                                 final HttpServletResponse response,
                                 final @RequestParam String code) {

        // 1. Grab user info from OneLogin
        final String jwt = oneLoginService.createOneLoginJwt();
        final String authToken = oneLoginService.getAuthToken(jwt, code);
        final JSONObject userInfo = oneLoginService.getUserInfo(authToken);
        final String sub = userInfo.getString("sub");
        final String email = userInfo.getString("email");

        // 2. Check if user is in the database
        boolean userExistsByEmail = oneLoginService.doesUserExistByEmail(email);
        boolean userExistsBySub = oneLoginService.doesUserExistBySub(sub);

        if(!userExistsByEmail) {
            // 3. If user is not in the database, create a new user
            oneLoginService.createUser(sub, email);
        } else if(!userExistsBySub) {
            // 4. If user is in the database, but has not used OneLogin before, update user info
            oneLoginService.addSubToUser(sub, email);
        }

        // 5. Create user service custom jwt from user info
        final Map<String, String> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("sub", sub);

        final Cookie userTokenCookie = WebUtil.buildCookie(
                new Cookie(userServiceCookieName, customJwtService.generateToken(claims)),
                Boolean.TRUE,
                Boolean.TRUE,
                null
        );

        response.addCookie(userTokenCookie);

        // 6. Check the users roles and redirect to the correct page
        final List<RoleEnum> usersRoles = oneLoginService.getUsersRoles(sub);

        boolean isSuperAdmin = usersRoles.stream().anyMatch((role) -> role.equals(RoleEnum.SUPERADMIN));
        if(isSuperAdmin) return new RedirectView("Super admin dashboard"); // TODO fetch url from somewhere

        boolean isAdmin = usersRoles.stream().anyMatch((role) -> role.equals(RoleEnum.ADMIN));
        if(isAdmin) return new RedirectView("Admin dashboard"); // TODO fetch url from somewhere

        return new RedirectView(redirectUrl.orElse(configProperties.getDefaultRedirectUrl()));
    }

}