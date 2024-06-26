package gov.cabinetoffice.gapuserservice.repository;

import gov.cabinetoffice.gapuserservice.model.Salt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SaltRepository extends JpaRepository<Salt, Integer> {
    Optional<Salt> findFirstBySaltIdOrderBySaltIdAsc(String saltId);
}

