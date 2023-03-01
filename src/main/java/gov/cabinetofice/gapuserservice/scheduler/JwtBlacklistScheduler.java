package gov.cabinetofice.gapuserservice.scheduler;

import gov.cabinetofice.gapuserservice.service.JwtBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class JwtBlacklistScheduler {

    private final JwtBlacklistService jwtBlacklistService;

    @Scheduled(cron = "${blacklist-scheduler.cronExpression:0 0 0 * * ?}", zone = "Europe/London")
    @SchedulerLock(name = "blacklist_deleteExpiredJwtScheduler",
            lockAtMostFor = "${blacklist-scheduler.lock.atMostFor:30m}",
            lockAtLeastFor = "${blacklist-scheduler.lock.atLeastFor:5m}")
    public void deleteExpiredJwts() {
        final Long deletedJwts = jwtBlacklistService.deleteExpiredJwts();
        log.info("Number of expired JWTs deleted: " + deletedJwts);
    }
}
