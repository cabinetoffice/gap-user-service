package gov.cabinetofice.gapuserservice.repository;

import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    @EntityGraph(attributePaths = {"department", "roles"})
    Optional<User> findByEmailAddress(String email);
    @EntityGraph(attributePaths = {"department", "roles"})
    Optional<User> findBySub(String sub);

    @EntityGraph(attributePaths = {"department", "roles"})
    Optional<User> findById(int id);

    @EntityGraph(attributePaths = {"department", "roles"})
    List<User> findByRoles_NameInOrDepartment_NameIn(
            @Param("roles") List<RoleEnum> roles,
            @Param("dept") List<String> dept,
            Pageable pageable
    );


    @Query(value = "SELECT *, levenshtein(email, :emailQuery) \n" +
            "FROM gap_users \n" +
            "ORDER BY levenshtein(email, :emailQuery) ASC \n" +
            "LIMIT 30",
            nativeQuery = true)
    List<User> findAllUsersByFuzzySearchOnEmailAddress(
            @Param("emailQuery") String emailQuery
    );
}
