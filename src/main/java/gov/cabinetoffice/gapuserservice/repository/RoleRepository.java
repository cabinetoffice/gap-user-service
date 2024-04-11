package gov.cabinetoffice.gapuserservice.repository;

import gov.cabinetoffice.gapuserservice.model.Role;
import gov.cabinetoffice.gapuserservice.model.RoleEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(RoleEnum name);
}

