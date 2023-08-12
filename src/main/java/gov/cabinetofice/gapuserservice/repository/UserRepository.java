package gov.cabinetofice.gapuserservice.repository;

import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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


    @Query(value = """
            SELECT *
            FROM gap_users
            ORDER BY levenshtein(email, :emailQuery) ASC
            """,
            nativeQuery = true)
    Page<User> findAllUsersByFuzzySearchOnEmailAddress(
            @Param("emailQuery") String emailQuery,
            Pageable pageable
    );

    @Query(value = """
                 SELECT * FROM gap_users INNER JOIN roles_users ON gap_users.gap_user_id = roles_users.users_gap_user_id
                  WHERE roles_users.roles_id IN :roleIds
                  AND dept_id IN :departmentIds
                  ORDER BY levenshtein(email, :emailQuery) ASC
            """,
            nativeQuery = true)
    Page<User> findUsersByDepartmentAndRolesAndFuzzySearchOnEmailAddress(
            @Param("roleIds") Collection<Integer> roleIds,
            @Param("departmentIds") Collection<Integer> departmentIds,
            @Param("emailQuery") String emailQuery,
            Pageable pageable
    );

    @Query(value = """
        SELECT * FROM gap_users
        WHERE dept_id IN :departmentIds
        ORDER BY levenshtein(email, :emailQuery) ASC
        """,
            nativeQuery = true)
    Page<User> findUsersByDepartmentAndFuzzySearchOnEmailAddress(
            @Param("departmentIds") Collection<Integer> departmentIds,
            @Param("emailQuery") String emailQuery,
            Pageable pageable
    );

    @Query(value = """
                  SELECT * FROM gap_users INNER JOIN roles_users ON gap_users.gap_user_id = roles_users.users_gap_user_id
                  WHERE roles_users.roles_id IN :roleIds
                  ORDER BY levenshtein(email, :emailQuery) ASC
           """,
            nativeQuery = true)
    Page<User> findUsersByRolesAndFuzzySearchOnEmailAddress(
            @Param("roleIds") Collection<Integer> roleIds,
            @Param("emailQuery") String emailQuery,
            Pageable pageable
    );

    @Query("""
            select u from User u inner join u.roles roles
            where roles.id in :roleIds
            and u.department.id in :departmentIds
            order by u.emailAddress
            """)
    Page<User> findUsersByDepartmentAndRoles(
            @Param("roleIds") Collection<Integer> roleIds,
            @Param("departmentIds") Collection<Integer> departmentIds,
            Pageable pageable
    );

    @Query("""
            select u from User u
            where u.department.id in :departmentIds
            order by u.emailAddress
            """)
    Page<User> findUsersByDepartment(
            @Param("departmentIds") Collection<Integer> departmentIds,
            Pageable pageable
    );

    @Query("""
            select u from User u inner join u.roles roles
            where roles.id in :roleIds
            order by u.emailAddress
            """)
    Page<User> findUsersByRoles(
            @Param("roleIds") Collection<Integer> roleIds,
            Pageable pageable
    );
}
