package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.DepartmentDto;
import gov.cabinetofice.gapuserservice.dto.RoleDto;
import gov.cabinetofice.gapuserservice.dto.SuperAdminDashboardPageDto;
import gov.cabinetofice.gapuserservice.dto.UserDto;
import gov.cabinetofice.gapuserservice.model.Department;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.DepartmentService;
import gov.cabinetofice.gapuserservice.service.RoleService;
import gov.cabinetofice.gapuserservice.service.user.OneLoginUserService;
import gov.cabinetofice.gapuserservice.web.controlleradvice.Error;
import gov.cabinetofice.gapuserservice.web.controlleradvice.ErrorResponseBody;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
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
        when(oneLoginUserService.getPaginatedUsers(pagination, "")).thenReturn(users);
        when(oneLoginUserService.getUserCount()).thenReturn(2L);

        ResponseEntity<SuperAdminDashboardPageDto> result =
                (ResponseEntity<SuperAdminDashboardPageDto>) superAdminController.superAdminDashboard(httpRequest,  pagination, null, null, "", false);
                Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());

        SuperAdminDashboardPageDto responseDto = result.getBody();
        Assertions.assertEquals(departments, Objects.requireNonNull(responseDto).getDepartments());
        Assertions.assertEquals(roles, responseDto.getRoles());
        Assertions.assertEquals(users, responseDto.getUsers());
        Assertions.assertEquals(2L, responseDto.getUserCount());
    }


    @Test
    void shouldReturnFilteredSuperAdminDashboardDto() {
        Pageable pagination = mock(Pageable.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        List<DepartmentDto> departments = List.of(DepartmentDto.builder().id("1").build(), DepartmentDto.builder().id("2").build());
        List<RoleDto> roles = List.of(RoleDto.builder().id("1").build(), RoleDto.builder().id("2").build());
        List<UserDto> users = List.of(
                new UserDto(User.builder().gapUserId(1).department(Department.builder().id(1).build()).roles(List.of(Role.builder().id(1).name(RoleEnum.SUPER_ADMIN).build())).build()),
                new UserDto(User.builder().gapUserId(2).department(Department.builder().id(2).build()).roles(List.of(Role.builder().id(2).name(RoleEnum.APPLICANT).build())).build())
        );

        when(departmentService.getAllDepartments()).thenReturn(departments);
        when(roleService.getAllRoles()).thenReturn(roles);
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        when(oneLoginUserService.getPaginatedUsers(pagination, "")).thenReturn(users);
        when(oneLoginUserService.getUserCount()).thenReturn(2L);

        ResponseEntity<SuperAdminDashboardPageDto> result =
                (ResponseEntity<SuperAdminDashboardPageDto>) superAdminController.superAdminDashboard(httpRequest,  pagination, "1", "1", "", false);

        SuperAdminDashboardPageDto responseDto = result.getBody();
        Assertions.assertEquals(1, Objects.requireNonNull(responseDto).getUsers().size());
        Assertions.assertEquals(1L, responseDto.getUserCount());
    }

    @Test
    void shouldReturnErrorResponseWhenSearchTermIsTooLong() {
        Pageable pagination = mock(Pageable.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        String searchTerm = "a".repeat(256);
        List<DepartmentDto> departments = List.of(DepartmentDto.builder().id("1").build(), DepartmentDto.builder().id("2").build());
        List<RoleDto> roles = List.of(RoleDto.builder().id("1").build(), RoleDto.builder().id("2").build());

        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);

        ResponseEntity<?> result = superAdminController.superAdminDashboard(httpRequest, pagination, null, null, searchTerm, false);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());

        ErrorResponseBody errorResponseBody = (ErrorResponseBody) result.getBody();
        Assertions.assertNotNull(errorResponseBody);
        Assertions.assertFalse(errorResponseBody.isResponseAccepted());
        Assertions.assertEquals("Search term must be less than 255 characters", errorResponseBody.getMessage());

        List<Error> errors = errorResponseBody.getErrors();
        Assertions.assertNotNull(errors);
        Assertions.assertEquals(1, errors.size());

        Error error = errors.get(0);
        Assertions.assertNotNull(error);
        Assertions.assertEquals("searchTerm", error.getFieldName());
        Assertions.assertEquals("Search term must be less than 255 characters", error.getErrorMessage());
    }
}