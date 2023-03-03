package gov.cabinetofice.gapuserservice.scheduler;

import gov.cabinetofice.gapuserservice.service.JwtBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtBlacklistScheduler {

    private final JwtBlacklistService jwtBlacklistService;

    @Scheduled(cron = "${blacklist-scheduler.cronExpression:0 0 0 * * ?}", zone = "Europe/London")
    @SchedulerLock(name = "blacklist_deleteExpiredJwtScheduler",
            lockAtMostFor = "${blacklist-scheduler.lock.atMostFor:30m}",
            lockAtLeastFor = "${blacklist-scheduler.lock.atLeastFor:5m}")
    public void deleteExpiredJwts() {
        log.info("Started job to delete expired tokens from the blacklist");
        final Long deletedJwts = jwtBlacklistService.deleteExpiredJwts();
        log.info("Number of expired JWTs deleted: " + deletedJwts);
        log.info("Finished job to delete expired tokens from the blacklist");
    }
}
