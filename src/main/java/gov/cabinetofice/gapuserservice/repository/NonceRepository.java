package gov.cabinetofice.gapuserservice.repository;

import gov.cabinetofice.gapuserservice.model.Nonce;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NonceRepository extends JpaRepository<Nonce, Integer> {
    Optional<Nonce> findFirstByNonceStringOrderByNonceStringAsc(String nonceString);
}

