package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.*;
import gov.cabinetofice.gapuserservice.model.Department;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.DepartmentService;
import gov.cabinetofice.gapuserservice.service.RoleService;
import gov.cabinetofice.gapuserservice.service.user.OneLoginUserService;
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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
        UserQueryDto userQueryDto = new UserQueryDto(List.of(departmentIds), List.of(roleIds), "");

        when(departmentService.getAllDepartments()).thenReturn(departments);
        when(roleService.getAllRoles()).thenReturn(roles);
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        when(oneLoginUserService.getPaginatedUsers(pagination, userQueryDto)).thenReturn(pagedUsers);

        List<UserAndRelationsDto> dtoUsers = users.stream().map(UserAndRelationsDto::new).collect(Collectors.toList());

        SuperAdminDashboardPageDto expectedResponseDto = SuperAdminDashboardPageDto.builder().departments(departments).roles(roles).users(dtoUsers).build();

        ResponseEntity<SuperAdminDashboardPageDto> result = superAdminController.superAdminDashboard(
                httpRequest, pagination, departmentIds, roleIds, ""
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
        UserQueryDto userQueryDto = new UserQueryDto(List.of(departmentIds), List.of(roleIds), "");

        when(departmentService.getAllDepartments()).thenReturn(departments);
        when(roleService.getAllRoles()).thenReturn(roles);
        when(roleService.isSuperAdmin(httpRequest)).thenReturn(true);
        when(oneLoginUserService.getPaginatedUsers(pagination, userQueryDto
        )).thenReturn(pageUsers);


        ResponseEntity<SuperAdminDashboardPageDto> result =
                superAdminController.superAdminDashboard(
                        httpRequest, pagination, departmentIds, roleIds, ""
                );

        SuperAdminDashboardPageDto responseDto = result.getBody();
        Assertions.assertNotNull(responseDto);
        Assertions.assertEquals(1, Objects.requireNonNull(responseDto).getUsers().size());
        Assertions.assertEquals(1L, responseDto.getUserCount());
    }
}