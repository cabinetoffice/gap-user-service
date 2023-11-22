package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.enums.SpotlightOAuthAuditType;
import gov.cabinetofice.gapuserservice.exceptions.ForbiddenException;
import gov.cabinetofice.gapuserservice.exceptions.InvalidRequestException;
import gov.cabinetofice.gapuserservice.model.SpotlightOAuthAudit;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.RoleService;
import gov.cabinetofice.gapuserservice.service.SpotlightService;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@RequiredArgsConstructor
@Controller
@RequestMapping("spotlight")
@Slf4j
public class SpotlightController {

    private final SpotlightService spotlightService;
    private final RoleService roleService;
    private final CustomJwtServiceImpl jwtService;

    private static final String NO_USER = "Could not get user from jwt";

    @GetMapping("/oauth/authorize")
    public RedirectView authorize(final HttpServletRequest httpRequest) throws Exception {
        log.info("SpotlightController /oauth/authorize");
        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }

        User user = jwtService.getUserFromJwt(httpRequest)
                .orElseThrow(() -> new InvalidRequestException(NO_USER));

        SpotlightOAuthAudit spotlightOAuthAudit = SpotlightOAuthAudit.builder()
                .user(user)
                .type(SpotlightOAuthAuditType.AUTHORISE)
                .build();

        spotlightService.saveAudit(spotlightOAuthAudit);
        String authUrl = spotlightService.getAuthorizeUrl();

        return new RedirectView(authUrl);
    }

    @GetMapping("/oauth/callback")
    public ResponseEntity callback(@RequestParam String code, @RequestParam String state, final HttpServletRequest httpRequest) {
        log.info("SpotlightController /oauth/callback");

        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }

        User user = jwtService.getUserFromJwt(httpRequest)
                .orElseThrow(() -> new InvalidRequestException(NO_USER));

        try {
            log.info("Spotlight authorization token successfully exchanged");
            spotlightService.exchangeAuthorizationToken(code, state);
            SpotlightOAuthAudit spotlightOAuthAudit = SpotlightOAuthAudit.builder()
                    .user(user)
                    .type(SpotlightOAuthAuditType.SUCCESS)
                    .build();

            spotlightService.saveAudit(spotlightOAuthAudit);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error exchanging Spotlight authorization token", e);

            SpotlightOAuthAudit spotlightOAuthAudit = SpotlightOAuthAudit.builder()
                    .user(user)
                    .type(SpotlightOAuthAuditType.FAILURE)
                    .build();

            spotlightService.saveAudit(spotlightOAuthAudit);

            return ResponseEntity.badRequest().body("Error exchanging Spotlight authorization token");
        }
    }


}
