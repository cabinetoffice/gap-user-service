package gov.cabinetofice.gapuserservice.repository;

import gov.cabinetofice.gapuserservice.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    @EntityGraph(attributePaths = {"department", "roles"})
    Optional<User> findByEmailAddress(String email);

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Query("SELECT u from User u inner join u.roles roles WHERE roles.id = :roleId and u.emailAddress = :emailAddress")
    Optional<User> findByEmailAddressAndRole(String emailAddress, Integer roleId);

    @Query("select u from User u inner join u.roles roles where roles.id = ?1")
    User findByRoles_Id(Integer id);

    @EntityGraph(attributePaths = {"department", "roles"})
    Optional<User> findBySub(String sub);

    List<User> findBySubIn(List<String> subs);

    @EntityGraph(attributePaths = {"department", "roles"})
    Optional<User> findByColaSub(UUID sub);

    @EntityGraph(attributePaths = {"department", "roles"})
    Optional<User> findById(int id);

    @Query("select u from User u order by u.emailAddress")
    Page<User> findByOrderByEmail(Pageable pageable);

    @Query(value = """
            SELECT *
                FROM
                    gap_users
                ORDER BY
                    similarity(email, :emailQuery) DESC
            """,
            nativeQuery = true)
    Page<User> findUsersByFuzzyEmail(
            @Param("emailQuery") String emailQuery,
            Pageable pageable
    );

    @Query(value = """
            SELECT DISTINCT ON (gap_user_id, similarity) *
            	FROM (
            	   select *,
            			  similarity(email, :emailQuery) as similarity
            	   FROM gap_users
            	) u
            	JOIN
                    roles_users r
                        ON r.users_gap_user_id = u.gap_user_id
                WHERE
                    r.roles_id IN :roleIds
                    AND u.dept_id IN :departmentIds
            	ORDER BY
            		similarity DESC
            """,
            nativeQuery = true)
    Page<User> findUsersByDepartmentAndRolesAndFuzzyEmail(
            @Param("roleIds") Collection<Integer> roleIds,
            @Param("departmentIds") Collection<Integer> departmentIds,
            @Param("emailQuery") String emailQuery,
            Pageable pageable
    );

    @Query(value = """
            SELECT *
                FROM
                    gap_users u
                WHERE
                    u.dept_id IN :departmentIds
                ORDER BY
                    similarity(email, :emailQuery) DESC
            """,
            nativeQuery = true)
    Page<User> findUsersByDepartmentAndFuzzyEmail(
            @Param("departmentIds") Collection<Integer> departmentIds,
            @Param("emailQuery") String emailQuery,
            Pageable pageable
    );

    @Query(value = """
            SELECT DISTINCT ON (gap_user_id, similarity) *
            	FROM (
            	   select *,
            			  similarity(email, :emailQuery) as similarity
            	   FROM gap_users
            	) u
            	JOIN
                    roles_users r
                        ON r.users_gap_user_id = u.gap_user_id
                WHERE
                    r.roles_id IN :roleIds
            	ORDER BY
            		similarity DESC
            """,
            nativeQuery = true)
    Page<User> findUsersByRolesAndFuzzyEmail(
            @Param("roleIds") Collection<Integer> roleIds,
            @Param("emailQuery") String emailQuery,
            Pageable pageable
    );

    @Query(value = """
            SELECT DISTINCT ON(gap_user_id, email) *
                FROM
                    gap_users u
                JOIN
                    roles_users r
                        ON r.users_gap_user_id = u.gap_user_id
                WHERE
                    r.roles_id IN :roleIds
                AND
                    u.dept_id in :departmentIds
                ORDER BY
                    u.email ASC
            """, nativeQuery = true)
    Page<User> findUsersByDepartmentAndRoles(
            @Param("roleIds") Collection<Integer> roleIds,
            @Param("departmentIds") Collection<Integer> departmentIds,
            Pageable pageable
    );

    @Query("""
            SELECT u
                FROM
                    User u
                WHERE
                    u.department.id in :departmentIds
                ORDER BY
                    u.emailAddress ASC
            """)
    Page<User> findUsersByDepartment(
            @Param("departmentIds") Collection<Integer> departmentIds,
            Pageable pageable
    );

    @Query(value = """
            SELECT DISTINCT ON(gap_user_id, email) *
                FROM
                    gap_users u
                JOIN
                    roles_users r
                        ON r.users_gap_user_id = u.gap_user_id
                WHERE
                    r.roles_id IN :roleIds
                ORDER BY
                    u.email ASC
            """, nativeQuery = true)
    Page<User> findUsersByRoles(
            @Param("roleIds") Collection<Integer> roleIds,
            Pageable pageable
    );
}
