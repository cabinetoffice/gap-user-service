package gov.cabinetoffice.gapuserservice.repository;

import gov.cabinetoffice.gapuserservice.model.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface JwtBlacklistRepository extends JpaRepository<BlacklistedToken, String> {

    long deleteByExpiryDateLessThan(LocalDateTime expiryDate);

    boolean existsByJwtIs(String jwt);
}
