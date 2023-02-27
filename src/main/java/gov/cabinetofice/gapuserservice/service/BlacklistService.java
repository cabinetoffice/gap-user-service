package gov.cabinetofice.gapuserservice.service;

import gov.cabinetofice.gapuserservice.exceptions.NotFoundException;
import gov.cabinetofice.gapuserservice.model.Blacklist;
import gov.cabinetofice.gapuserservice.repository.BlacklistRepository;
import gov.cabinetofice.gapuserservice.service.jwt.impl.ColaJwtServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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


    public Long deleteExpiredJwts() {
        return this.blacklistRepository.deleteByExpiryDateLessThan(Calendar.getInstance().getTime());
    }
}
