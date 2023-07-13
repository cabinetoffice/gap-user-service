package gov.cabinetofice.gapuserservice.service;


import gov.cabinetofice.gapuserservice.dto.DeptDto;
import gov.cabinetofice.gapuserservice.mappers.DepartmentMapper;
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
    public List<DeptDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(departmentMapper::deptToDeptDto)
                .collect(Collectors.toList());
    }
}
