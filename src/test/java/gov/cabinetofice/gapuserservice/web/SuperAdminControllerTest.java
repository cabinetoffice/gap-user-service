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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.eq;
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
        List<DepartmentDto> departments = List.of(
                DepartmentDto.builder().id(1).build(),
                DepartmentDto.builder().id(2).build()
        );
        List<RoleDto> roles = List.of(
                RoleDto.builder().id(1).build(),
                RoleDto.builder().id(2).build()
        );

        List<User> users = List.of(
                User.builder().gapUserId(1).build(),
                User.builder().gapUserId(2).build()
        );
        Page<User> pagedUsers = new PageImpl<>(users);

        Integer[] departmentIds = {1};
        Integer[] roleIds = {1};

        when(departmentService.getAllDepartments()).thenReturn(departments);
        when(roleService.getAllRoles()).thenReturn(roles);
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        when(oneLoginUserService.getPaginatedUsers(eq(pagination), eq(""), eq(Arrays.asList(departmentIds)), eq(Arrays.asList(roleIds)))).thenReturn(pagedUsers);

        List<UserDto> dtoUsers = users.stream().map(UserDto::new).collect(Collectors.toList());

        SuperAdminDashboardPageDto expectedResponseDto = SuperAdminDashboardPageDto.builder().departments(departments).roles(roles).users(dtoUsers).build();

        ResponseEntity<SuperAdminDashboardPageDto> result = superAdminController.superAdminDashboard(
                httpRequest, pagination, departmentIds, roleIds, "", false
        );

        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());

        SuperAdminDashboardPageDto responseDto = result.getBody();
        Assertions.assertNotNull(responseDto);
        Assertions.assertEquals(expectedResponseDto.getDepartments(), responseDto.getDepartments());
        Assertions.assertEquals(expectedResponseDto.getRoles(), responseDto.getRoles());
        Assertions.assertEquals(expectedResponseDto.getUsers(), responseDto.getUsers());
    }

    @Test
    void shouldReturnFilteredSuperAdminDashboardDto() {
        Pageable pagination = mock(Pageable.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        List<DepartmentDto> departments = List.of(
                DepartmentDto.builder().id(1).build(),
                DepartmentDto.builder().id(2).build()
        );
        List<RoleDto> roles = List.of(
                RoleDto.builder().id(1).build(),
                RoleDto.builder().id(2).build()
        );
        List<User> users = List.of(
                User.builder()
                        .gapUserId(1)
                        .department(Department.builder().id(1).build())
                        .roles(List.of(Role.builder().id(1).name(RoleEnum.SUPER_ADMIN).build()))
                        .build(),
                User.builder()
                        .gapUserId(2)
                        .department(Department.builder().id(2).build())
                        .roles(List.of(Role.builder().id(2).name(RoleEnum.APPLICANT).build()))
                        .build()
        );
        Page<User> pageUsers = new PageImpl<>(List.of(users.get(0)));

        Integer[] departmentIds = {1};
        Integer[] roleIds = {1};

        when(departmentService.getAllDepartments()).thenReturn(departments);
        when(roleService.getAllRoles()).thenReturn(roles);
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        when(oneLoginUserService.getPaginatedUsers(
                eq(pagination), eq(""), eq(Arrays.asList(departmentIds)), eq(Arrays.asList(roleIds))
        )).thenReturn(pageUsers);


        ResponseEntity<SuperAdminDashboardPageDto> result =
                superAdminController.superAdminDashboard(
                        httpRequest, pagination, departmentIds, roleIds, "", false
                );

        SuperAdminDashboardPageDto responseDto = result.getBody();
        Assertions.assertNotNull(responseDto);
        Assertions.assertEquals(1, Objects.requireNonNull(responseDto).getUsers().size());
        Assertions.assertEquals(1L, responseDto.getUserCount());
    }
    @Test
    void shouldReturnErrorResponseWhenSearchTermIsTooLong() {
        Pageable pagination = mock(Pageable.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        String searchTerm = "e".repeat(256);
        List<DepartmentDto> departments = List.of(
                DepartmentDto.builder().id(1).build(),
                DepartmentDto.builder().id(2).build()
        );
        List<RoleDto> roles = List.of(
                RoleDto.builder().id(1).build(),
                RoleDto.builder().id(2).build()
        );

        Integer[] departmentIds = {1};
        Integer[] roleIds = {1};

        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);

        ResponseEntity<?> result = superAdminController.superAdminDashboard(
                httpRequest, pagination, departmentIds, roleIds, searchTerm, false
        );

        // Assertions
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