package gov.cabinetofice.gapuserservice.service;


import gov.cabinetofice.gapuserservice.dto.DepartmentDto;
import gov.cabinetofice.gapuserservice.exceptions.DepartmentNotFoundException;
import gov.cabinetofice.gapuserservice.mappers.DepartmentMapper;
import gov.cabinetofice.gapuserservice.model.Department;
import gov.cabinetofice.gapuserservice.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;
    public List<DepartmentDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(departmentMapper::departmentToDepartmentDto)
                .collect(Collectors.toList());
    }

    public Optional<Department> getDepartmentById(int id) {
        return departmentRepository.findById(id);
    }

    public Department updateDepartment(Department department, String departmentName, String ggisId) throws DepartmentNotFoundException {
         department.setName(departmentName);
         department.setGgisID(ggisId);
        departmentRepository.save(department);
        return department;
    }

    public Void deleteDepartment(int id) {
        Department department = departmentRepository.findById(id).orElseThrow(() -> new DepartmentNotFoundException("Department not found"));
        departmentRepository.delete(department);
        return null;
    }

    public void createDepartment(String name, String ggisIDs) {
        Department department = new Department();
        department.setName(name);
        department.setGgisID(ggisIDs);
        departmentRepository.save(department);
    }
}
