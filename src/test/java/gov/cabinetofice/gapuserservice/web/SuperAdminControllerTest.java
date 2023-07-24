package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.DepartmentDto;
import gov.cabinetofice.gapuserservice.dto.RoleDto;
import gov.cabinetofice.gapuserservice.dto.SuperAdminDashboardPageDto;
import gov.cabinetofice.gapuserservice.dto.UserDto;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.DepartmentService;
import gov.cabinetofice.gapuserservice.service.RoleService;
import gov.cabinetofice.gapuserservice.service.user.OneLoginUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminControllerTest {
    @Mock
    private DepartmentService departmentService;

    @Mock
    private RoleService roleService;

    @Mock
    private OneLoginUserService oneLoginUserService;

    @InjectMocks
    private SuperAdminController superAdminController;

    @Test
    void shouldReturnSuperAdminDashboardDto() {
        Pageable pagination = mock(Pageable.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        List<DepartmentDto> departments = List.of(DepartmentDto.builder().id("1").build(),
                DepartmentDto.builder().id("2").build());
        List<RoleDto> roles = List.of(RoleDto.builder().id("1").build(), RoleDto.builder().id("2").build());
        List<UserDto> users = List.of(
                new UserDto(User.builder().gapUserId(1).build()),
                new UserDto(User.builder().gapUserId(2).build())
        );

        when(departmentService.getAllDepartments()).thenReturn(departments);
        when(roleService.getAllRoles()).thenReturn(roles);
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        when(oneLoginUserService.getPaginatedUsers(pagination)).thenReturn(users);
        when(oneLoginUserService.getUserCount()).thenReturn(2L);

        ResponseEntity<SuperAdminDashboardPageDto> result =
                superAdminController.superAdminDashboard(httpRequest, pagination);

        assertEquals(HttpStatus.OK, result.getStatusCode());

        SuperAdminDashboardPageDto responseDto = result.getBody();
        assertEquals(departments, Objects.requireNonNull(responseDto).getDepartments());
        assertEquals(roles, responseDto.getRoles());
        assertEquals(users, responseDto.getUsers());
        assertEquals(2L, responseDto.getUserCount());
    }
}