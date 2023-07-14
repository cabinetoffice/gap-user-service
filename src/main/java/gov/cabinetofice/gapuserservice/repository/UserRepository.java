package gov.cabinetofice.gapuserservice.repository;

import gov.cabinetofice.gapuserservice.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    @EntityGraph(attributePaths = {"department", "roles"})
    Optional<User> findByEmail(String email);
    @EntityGraph(attributePaths = {"department", "roles"})
    Optional<User> findBySub(String sub);

    @EntityGraph(attributePaths = {"department", "roles"})
    Optional<User> findById(int id);

    void deleteByRoles_Id(Integer id);

}

