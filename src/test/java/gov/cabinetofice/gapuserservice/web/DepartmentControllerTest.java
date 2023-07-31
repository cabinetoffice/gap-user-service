package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.DepartmentDto;
import gov.cabinetofice.gapuserservice.dto.UpdateDepartmentReqDto;
import gov.cabinetofice.gapuserservice.model.Department;
import gov.cabinetofice.gapuserservice.service.DepartmentService;
import gov.cabinetofice.gapuserservice.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

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
        final DepartmentDto department = DepartmentDto.builder().ggisID("ggis").name("Cabinet office").id("1").build();
        when(departmentService.getDepartmentById(1))
                .thenReturn(Optional.of(Department.builder().ggisID("ggis").name("Cabinet office").id(1).build()));
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        final ResponseEntity<DepartmentDto> methodResponse = departmentController.getById( 1, httpRequest);
        assertThat(methodResponse.getBody()).isEqualToComparingFieldByFieldRecursively(department);
    }

    @Test
    void updateDepartment() {
        ArgumentCaptor<Department> departmentArgumentCaptor = ArgumentCaptor.forClass(Department.class);
        final UpdateDepartmentReqDto body = new UpdateDepartmentReqDto();
        body.setDepartmentName("Cabinet office");
        body.setGgisId("initial ggis id");

        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        when(departmentService.getDepartmentById(1)).thenReturn(Optional.of(Department.builder().id(1).build()));

        when(departmentService.updateDepartment(departmentArgumentCaptor.capture(), eq("Cabinet office") ,eq("initial ggis id"))).thenReturn(Department.builder().id(1).build());

        final ResponseEntity methodResponse = departmentController.updateDepartment(httpRequest, body, 1);

        assertThat(methodResponse.getBody()).isSameAs("Department updated");
    }
}

