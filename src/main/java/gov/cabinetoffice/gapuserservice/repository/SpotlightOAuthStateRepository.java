package gov.cabinetoffice.gapuserservice.repository;

import gov.cabinetoffice.gapuserservice.model.SpotlightOAuthState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpotlightOAuthStateRepository extends JpaRepository<SpotlightOAuthState, Integer> {
    SpotlightOAuthState findFirstBy();
}
