package gov.cabinetofice.gapuserservice.repository;

import gov.cabinetofice.gapuserservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByEmailAddress(String email);
    Optional<User> findByEmailAddress(String email);
    boolean existsBySub(String sub);
    Optional<User> findBySub(String sub);
    void deleteByRoles_Id(Integer id);
}

