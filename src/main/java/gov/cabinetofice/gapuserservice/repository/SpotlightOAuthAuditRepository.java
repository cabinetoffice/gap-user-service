package gov.cabinetofice.gapuserservice.repository;

import gov.cabinetofice.gapuserservice.enums.SpotlightOAuthAuditStatus;
import gov.cabinetofice.gapuserservice.model.SpotlightOAuthAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpotlightOAuthAuditRepository extends JpaRepository<SpotlightOAuthAudit, Integer> {

    SpotlightOAuthAudit findFirstByStatusOrStatusOrderByIdDesc(SpotlightOAuthAuditStatus status, SpotlightOAuthAuditStatus status1);

}