package gov.cabinetofice.gapuserservice.repositories;

import gov.cabinetofice.gapuserservice.model.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface TokenBlacklistRepository extends JpaRepository<BlacklistedToken, Integer> {

    boolean existsByToken(String token);

    int deleteByExpiryLessThanEqual(LocalDateTime dateTime);
}
