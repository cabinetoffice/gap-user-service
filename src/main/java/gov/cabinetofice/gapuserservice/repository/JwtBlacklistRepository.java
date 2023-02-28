package gov.cabinetofice.gapuserservice.repository;

import gov.cabinetofice.gapuserservice.model.JwtBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface JwtBlacklistRepository extends JpaRepository <JwtBlacklist, String> {

    long deleteByExpiryDateLessThan(LocalDateTime expiryDate);

    boolean existsByJwtIs(String jwt);
}
