package gov.cabinetofice.gapuserservice.repository;

import gov.cabinetofice.gapuserservice.model.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface JwtBlacklistRepository extends JpaRepository <BlacklistedToken, String> {

    long deleteByExpiryDateLessThan(LocalDateTime expiryDate);

    boolean existsByJwtIs(String jwt);
}
