package gov.cabinetofice.gapuserservice.service;

import gov.cabinetofice.gapuserservice.model.Blacklist;
import gov.cabinetofice.gapuserservice.repository.BlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Calendar;

@RequiredArgsConstructor
@Service
public class BlacklistService {
    private final BlacklistRepository blacklistRepository;

    public void addJwtToBlacklist(final String jwt) {
        Blacklist blacklist = Blacklist.builder()
                .jwt(jwt)
                .build();
        this.blacklistRepository.save(blacklist);
    }

    public boolean isJwtInBlacklist(final String jwt) {
        return blacklistRepository.existsByJwtIs(jwt);
    }

    public Long deleteExpiredJwts() {
        return this.blacklistRepository.deleteByExpiryDateLessThan(Calendar.getInstance().getTime());
    }
}
