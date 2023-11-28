package gov.cabinetofice.gapuserservice.scheduler;

import gov.cabinetofice.gapuserservice.enums.SpotlightOAuthAuditEvent;
import gov.cabinetofice.gapuserservice.enums.SpotlightOAuthAuditStatus;
import gov.cabinetofice.gapuserservice.model.SpotlightOAuthAudit;
import gov.cabinetofice.gapuserservice.service.SpotlightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotlightOAuthRefreshScheduler {

    private final SpotlightService spotlightService;

    @Scheduled(cron = "${spotlight.scheduler.cronExpression:0 0 20 * * *}", zone = "Europe/London")
    @SchedulerLock(name = "spotlight_oauth_refresh_scheduler",
            lockAtMostFor = "${spotlight.scheduler.lock.atMostFor:5m}",
            lockAtLeastFor = "${spotlight.scheduler.lock.atLeastFor:1m}")
    public void spotlightOAuthTokenRefreshSchedule() {
        log.info("Started Spotlight OAuth Token Refresh Scheduler");
        SpotlightOAuthAudit spotlightOAuthAudit = SpotlightOAuthAudit.builder()
                .event(SpotlightOAuthAuditEvent.REFRESH)
                .status(SpotlightOAuthAuditStatus.REQUEST)
                .build();

        spotlightService.saveAudit(spotlightOAuthAudit);

        try {
            spotlightService.refreshToken();

            log.info("Spotlight authorization token successfully refreshed");
            SpotlightOAuthAudit spotlightOAuthAuditResult = SpotlightOAuthAudit.builder()
                    .event(SpotlightOAuthAuditEvent.REFRESH)
                    .status(SpotlightOAuthAuditStatus.SUCCESS)
                    .build();

            spotlightService.saveAudit(spotlightOAuthAuditResult);

        } catch (Exception e) {
            log.error("Error refreshing Spotlight authorization token", e);

            SpotlightOAuthAudit spotlightOAuthAuditResult = SpotlightOAuthAudit.builder()
                    .event(SpotlightOAuthAuditEvent.REFRESH)
                    .status(SpotlightOAuthAuditStatus.FAILURE)
                    .build();

            spotlightService.saveAudit(spotlightOAuthAuditResult);
        }
        log.info("Finished Spotlight OAuth Token Refresh Scheduler");
    }
}
