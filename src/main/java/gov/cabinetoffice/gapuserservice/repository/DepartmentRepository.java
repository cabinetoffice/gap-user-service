package gov.cabinetoffice.gapuserservice.repository;

import gov.cabinetoffice.gapuserservice.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {
}
