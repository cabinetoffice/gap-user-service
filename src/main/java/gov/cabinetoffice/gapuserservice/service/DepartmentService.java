package gov.cabinetoffice.gapuserservice.service;


import gov.cabinetoffice.gapuserservice.dto.DepartmentDto;
import gov.cabinetoffice.gapuserservice.exceptions.DepartmentNotFoundException;
import gov.cabinetoffice.gapuserservice.mappers.DepartmentMapper;
import gov.cabinetoffice.gapuserservice.model.Department;
import gov.cabinetoffice.gapuserservice.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    public List<DepartmentDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(departmentMapper::departmentToDepartmentDto)
                .toList();
    }

    public Optional<Department> getDepartmentById(int id) {
        return departmentRepository.findById(id);
    }

    public Department updateDepartment(Department department, String departmentName, String ggisId) throws DepartmentNotFoundException {
        department.setName(departmentName);
        department.setGgisID(ggisId);
        return departmentRepository.save(department);
    }

    public void deleteDepartment(int id) {
        departmentRepository.deleteById(id);
    }

    public Department createDepartment(String departmentName, String ggisIDs) {
        Department department = Department.builder().name(departmentName).ggisID(ggisIDs).build();
        return departmentRepository.save(department);
    }
}
