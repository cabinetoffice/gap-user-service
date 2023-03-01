package gov.cabinetofice.gapuserservice.service;

import gov.cabinetofice.gapuserservice.model.JwtBlacklist;
import gov.cabinetofice.gapuserservice.repository.JwtBlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.ZonedDateTime;

@RequiredArgsConstructor
@Service
public class JwtBlacklistService {
    private final JwtBlacklistRepository jwtBlacklistRepository;

    private final Clock clock;

    public void addJwtToBlacklist(final String jwt) {
        JwtBlacklist blacklist = JwtBlacklist.builder()
                .jwt(jwt)
                .build();
        this.jwtBlacklistRepository.save(blacklist);
    }

    public boolean isJwtInBlacklist(final String jwt) {
        return jwtBlacklistRepository.existsByJwtIs(jwt);
    }

    public Long deleteExpiredJwts() {
        return this.jwtBlacklistRepository.deleteByExpiryDateLessThan(ZonedDateTime.now(clock).toLocalDateTime());
    }
}
