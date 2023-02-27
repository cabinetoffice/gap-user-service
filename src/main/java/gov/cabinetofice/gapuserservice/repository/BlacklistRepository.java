package gov.cabinetofice.gapuserservice.repository;

import gov.cabinetofice.gapuserservice.model.Blacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface BlacklistRepository extends JpaRepository <Blacklist, String> {

    long deleteByExpiryDateLessThan(Date expiryDate);

    boolean existsByJwtIs(String jwt);
}
