package gov.cabinetofice.gapuserservice.repository;

import gov.cabinetofice.gapuserservice.model.JwtBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;

public interface JwtBlacklistRepository extends JpaRepository <JwtBlacklist, String> {

    long deleteByExpiryDateLessThan(Date expiryDate);

    boolean existsByJwtIs(String jwt);
}
