package gov.cabinetofice.gapuserservice.repository;

import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(RoleEnum name);
    List<Role> findByUsers_EmailAddress(String email);
    List<Role> findByUsers_Sub(String sub);
    void deleteAllByUsers_Sub(String sub);

}

