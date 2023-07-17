package gov.cabinetofice.gapuserservice.service;

import gov.cabinetofice.gapuserservice.dto.DepartmentDto;
import gov.cabinetofice.gapuserservice.mappers.DepartmentMapper;
import gov.cabinetofice.gapuserservice.model.Department;
import gov.cabinetofice.gapuserservice.repository.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DepartmentServiceTest {
    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private DepartmentService departmentService;

    @Test
    void testGetAllDepartments() {
        Department department1 = Department.builder().id(1).build();
        Department department2 = Department.builder().id(2).build();
        List<Department> departments = List.of(department1, department2);
        DepartmentDto departmentDto1 = DepartmentDto.builder().id("1").build();
        DepartmentDto departmentDto2 = DepartmentDto.builder().id("2").build();
        List<DepartmentDto> expectedDepartments = List.of(departmentDto1, departmentDto2);

        when(departmentRepository.findAll()).thenReturn(departments);
        when(departmentMapper.departmentToDepartmentDto(department1)).thenReturn(departmentDto1);
        when(departmentMapper.departmentToDepartmentDto(department2)).thenReturn(departmentDto2);

        List<DepartmentDto> result = departmentService.getAllDepartments();

        assertEquals(expectedDepartments, result);
    }

}
