package gov.cabinetoffice.gapuserservice.repository;

import gov.cabinetoffice.gapuserservice.enums.SpotlightOAuthAuditStatus;
import gov.cabinetoffice.gapuserservice.model.SpotlightOAuthAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface SpotlightOAuthAuditRepository extends JpaRepository<SpotlightOAuthAudit, Integer> {

    SpotlightOAuthAudit findFirstByStatusInOrderByIdDesc(Collection<SpotlightOAuthAuditStatus> statuses);

}