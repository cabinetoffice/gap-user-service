package gov.cabinetofice.gapuserservice.repository;

import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(RoleEnum name);

    @Query(value = """
            SELECT id
            FROM roles
            WHERE id NOT IN :roleIds
            """,
            nativeQuery = true)
    List<Integer> findRoleIdsNotIn(@Param("roleIds") List<Integer> roleIds);
}

