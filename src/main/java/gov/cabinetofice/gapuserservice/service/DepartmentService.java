package gov.cabinetofice.gapuserservice.service;


import gov.cabinetofice.gapuserservice.dto.DepartmentDto;
import gov.cabinetofice.gapuserservice.exceptions.DepartmentNotFoundException;
import gov.cabinetofice.gapuserservice.mappers.DepartmentMapper;
import gov.cabinetofice.gapuserservice.model.Department;
import gov.cabinetofice.gapuserservice.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
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

    public Department getDepartmentById(int id) {
        return departmentRepository.findById(id).orElseThrow(() -> new DepartmentNotFoundException("Department not found"));
    }

    public Void updateDepartment(int id, String departmentName, String ggisId) throws DepartmentNotFoundException {
        Department department = departmentRepository.findById(id).orElseThrow(() -> new DepartmentNotFoundException("Department not found"));
         department.setName(departmentName);
         department.setGgisID(ggisId);
         departmentRepository.save(department);
        return null;
    }
}
