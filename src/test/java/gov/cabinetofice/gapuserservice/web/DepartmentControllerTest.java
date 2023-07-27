package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.DepartmentDto;
import gov.cabinetofice.gapuserservice.model.Department;
import gov.cabinetofice.gapuserservice.service.DepartmentService;
import gov.cabinetofice.gapuserservice.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DepartmentControllerTest {

    @InjectMocks
    private DepartmentController departmentController;
    @Mock
    private DepartmentService departmentService;
    @Mock
    private RoleService roleService;

    @Test
    void getAllDepartments_returnsArrayOfDepartments() {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        final List<DepartmentDto> departments = List.of(DepartmentDto.builder().name("Cabinet office").id("1").ggisID("ggis").build());
        when(departmentService.getAllDepartments())
                .thenReturn(departments);
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        final ResponseEntity<List<DepartmentDto>> methodResponse = departmentController.getAll(httpRequest);
        assertThat(methodResponse.getBody()).isSameAs(departments);
    }

    @Test
    void getById_returnsTheCorrectDepartment() {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        final Department department = Department.builder().name("Cabinet office").id(1).ggisID("ggis").build();
        when(departmentService.getDepartmentById(1))
                .thenReturn(department);
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        final ResponseEntity<Department> methodResponse = departmentController.getById( 1, httpRequest);
        assertThat(methodResponse.getBody()).isSameAs(department);
    }

    @Test
    void updateDepartment() {
        final LinkedHashMap body = new LinkedHashMap();
        body.put("departmentName", "Cabinet office");
        body.put("ggisId", "ggis");

        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        when(departmentService.updateDepartment(1, "Cabinet office" ,"ggis")).thenReturn(Department.builder().build());
        final ResponseEntity methodResponse = departmentController.updateDepartment(httpRequest, body, 1);

        assertThat(methodResponse.getBody()).isSameAs("Department updated");
    }
}

