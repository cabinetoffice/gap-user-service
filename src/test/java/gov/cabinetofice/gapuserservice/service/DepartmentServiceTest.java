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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {
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
        DepartmentDto departmentDto1 = DepartmentDto.builder().id(1).build();
        DepartmentDto departmentDto2 = DepartmentDto.builder().id(2).build();
        List<DepartmentDto> expectedDepartments = List.of(departmentDto1, departmentDto2);

        when(departmentRepository.findAll()).thenReturn(departments);
        when(departmentMapper.departmentToDepartmentDto(department1)).thenReturn(departmentDto1);
        when(departmentMapper.departmentToDepartmentDto(department2)).thenReturn(departmentDto2);

        List<DepartmentDto> result = departmentService.getAllDepartments();

        assertEquals(expectedDepartments, result);
    }
    @Test
    void testGetDepartmentById() {
        Optional<Department> department = Optional.of(Department.builder().id(1).build());
        when(departmentRepository.findById(1)).thenReturn(department);
        Optional<Department> result = departmentService.getDepartmentById(1);
        assertEquals(department, result);
    }
    @Test
    void testUpdateDepartment() {
        String newDepartmentName = "new department";
        String newGgisId = "new ggisid";
        Department expected = Department.builder().id(1).ggisID(newGgisId).name(newDepartmentName).build();

        Department initialDepartment = Department.builder().id(1).name("Cabinet Office").ggisID("1").build();
        when(departmentRepository.save(any(Department.class))).thenReturn(expected);
        Department result = departmentService.updateDepartment(initialDepartment, newDepartmentName, newGgisId);

        assertThat(result).isEqualToComparingFieldByFieldRecursively(expected);
    }

    @Test
    void testCreateDepartment() {
        String departmentName = "new department";
        String ggisId = "new ggisid";
        Department toBeInserted = Department.builder().name(departmentName).ggisID(ggisId).build();
        when(departmentRepository.save(any())).thenReturn(toBeInserted);
        Department toBeReturned = departmentService.createDepartment(departmentName, ggisId);
        assertThat(toBeReturned.getName()).isEqualTo(departmentName);
        assertThat(toBeReturned.getGgisID()).isEqualTo(ggisId);
    }

}
