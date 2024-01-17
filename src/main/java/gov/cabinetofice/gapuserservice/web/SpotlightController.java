package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.SpotlightIntegrationAuditDto;
import gov.cabinetofice.gapuserservice.enums.SpotlightOAuthAuditEvent;
import gov.cabinetofice.gapuserservice.enums.SpotlightOAuthAuditStatus;
import gov.cabinetofice.gapuserservice.exceptions.ForbiddenException;
import gov.cabinetofice.gapuserservice.exceptions.InvalidRequestException;
import gov.cabinetofice.gapuserservice.exceptions.SpotlightTokenException;
import gov.cabinetofice.gapuserservice.model.SpotlightOAuthAudit;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.RoleService;
import gov.cabinetofice.gapuserservice.service.SpotlightService;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Objects;

@RequiredArgsConstructor
@Controller
@RequestMapping("spotlight")
@ConditionalOnProperty(value = "feature.spotlight.enabled", havingValue = "true")
@Slf4j
public class SpotlightController {

    private final SpotlightService spotlightService;
    private final RoleService roleService;
    private final CustomJwtServiceImpl jwtService;

    private static final String NO_USER = "Could not get user from jwt";

    @Value("${admin-base-url}")
    private String adminBaseUrl;

    @GetMapping("/integration")
    public ResponseEntity<SpotlightIntegrationAuditDto> getIntegrations(final HttpServletRequest httpRequest) {
        log.info("SpotlightController /integration/");
        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }
        SpotlightOAuthAudit audit = spotlightService.getLatestAudit();
        if (Objects.equals(null, audit)) {
            throw new InvalidRequestException("No audit found");
        }
        SpotlightIntegrationAuditDto spotlightIntegrationAuditDto = new SpotlightIntegrationAuditDto(
                "Spotlight", audit.getId(), audit.getEvent(),
                audit.getStatus(), audit.getTimestamp());

        return ResponseEntity.ok(spotlightIntegrationAuditDto);
    }

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
                .event(SpotlightOAuthAuditEvent.AUTHORISE)
                .status(SpotlightOAuthAuditStatus.REQUEST)
                .build();

        spotlightService.saveAudit(spotlightOAuthAudit);
        String authUrl = spotlightService.getAuthorizeUrl();

        return new RedirectView(authUrl);
    }

    @GetMapping("/oauth/callback")
    public RedirectView callback(@RequestParam String code, @RequestParam String state, final HttpServletRequest httpRequest) {
        log.info("SpotlightController /oauth/callback");

        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }

        User user = jwtService.getUserFromJwt(httpRequest)
                .orElseThrow(() -> new InvalidRequestException(NO_USER));

        try {
            spotlightService.exchangeAuthorizationToken(code, state);

            log.info("Spotlight authorization token successfully exchanged");
            SpotlightOAuthAudit spotlightOAuthAudit = SpotlightOAuthAudit.builder()
                    .user(user)
                    .event(SpotlightOAuthAuditEvent.AUTHORISE)
                    .status(SpotlightOAuthAuditStatus.SUCCESS)
                    .build();

            spotlightService.saveAudit(spotlightOAuthAudit);

            return new RedirectView(adminBaseUrl + "?redirectUrl=/super-admin-dashboard");
        } catch (Exception e) {
            log.error("Error exchanging Spotlight authorization token", e);

            SpotlightOAuthAudit spotlightOAuthAudit = SpotlightOAuthAudit.builder()
                    .user(user)
                    .event(SpotlightOAuthAuditEvent.AUTHORISE)
                    .status(SpotlightOAuthAuditStatus.FAILURE)
                    .build();

            spotlightService.saveAudit(spotlightOAuthAudit);

            throw new SpotlightTokenException("Error exchanging Spotlight authorization token");
        }
    }


    @GetMapping("/oauth/refresh")
    public ResponseEntity<Void> refresh(final HttpServletRequest httpRequest) {
        log.info("SpotlightController /oauth/refresh");

        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }

        User user = jwtService.getUserFromJwt(httpRequest)
                .orElseThrow(() -> new InvalidRequestException(NO_USER));

        try {
            spotlightService.refreshToken();

            log.info("Spotlight authorization token successfully refreshed");
            SpotlightOAuthAudit spotlightOAuthAudit = SpotlightOAuthAudit.builder()
                    .user(user)
                    .event(SpotlightOAuthAuditEvent.REFRESH)
                    .status(SpotlightOAuthAuditStatus.SUCCESS)
                    .build();

            spotlightService.saveAudit(spotlightOAuthAudit);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error refreshing Spotlight authorization token", e);

            SpotlightOAuthAudit spotlightOAuthAudit = SpotlightOAuthAudit.builder()
                    .user(user)
                    .event(SpotlightOAuthAuditEvent.REFRESH)
                    .status(SpotlightOAuthAuditStatus.FAILURE)
                    .build();

            spotlightService.saveAudit(spotlightOAuthAudit);

            throw new SpotlightTokenException("Error refreshing Spotlight authorization token");
        }
    }


}
