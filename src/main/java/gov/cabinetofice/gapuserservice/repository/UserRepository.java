package gov.cabinetofice.gapuserservice.repository;

import gov.cabinetofice.gapuserservice.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    @EntityGraph(attributePaths = {"roles", "department"})
    Optional<User> findByEmail(String email);
    @EntityGraph(attributePaths = {"roles", "department"})
    Optional<User> findBySub(String sub);

    @Query("select u from User u where u.id is not null order by u.email")
    List<User> findPaginatedUsers(Pageable pageable);


}

