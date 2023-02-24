package gov.cabinetofice.gapuserservice.repositories;

import gov.cabinetofice.gapuserservice.model.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenBlacklistRepository extends JpaRepository<BlacklistedToken, Integer> {

    boolean existsByToken(String token);
}
