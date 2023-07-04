package gov.cabinetofice.gapuserservice.repository;

import gov.cabinetofice.gapuserservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByHashedEmail(String hashedEmail);
    boolean existsBySub(String sub);
    Optional<User> findByHashedEmail(String hashedEmail);
    Optional<User> findBySub(String sub);
}

