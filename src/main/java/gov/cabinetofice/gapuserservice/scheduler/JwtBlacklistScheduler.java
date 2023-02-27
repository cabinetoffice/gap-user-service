package gov.cabinetofice.gapuserservice.scheduler;

import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component("jwtBlacklistScheduler")
@RequiredArgsConstructor
public class JwtBlacklistScheduler {

    private final CustomJwtServiceImpl jwtService;
    public static final String QUARTER_PAST_MIDNIGHT_CRON = "0 15 00 ? * *";

    @Scheduled(cron = QUARTER_PAST_MIDNIGHT_CRON)
    public void clearExpiredTokensFromBlacklist() {
        log.info("Started job to delete expired tokens from the blacklist.");
        jwtService.deleteExpiredTokensFromBlacklist();
        log.info("Finished job to delete expired tokens from the blacklist.");
    }
}
