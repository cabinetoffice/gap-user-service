package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.DepartmentDto;
import gov.cabinetofice.gapuserservice.dto.RoleDto;
import gov.cabinetofice.gapuserservice.dto.SuperAdminDashboardPageDto;
import gov.cabinetofice.gapuserservice.dto.UserDto;
import gov.cabinetofice.gapuserservice.exceptions.ForbiddenException;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.DepartmentService;
import gov.cabinetofice.gapuserservice.service.RoleService;
import gov.cabinetofice.gapuserservice.service.user.OneLoginUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;


@RequiredArgsConstructor
@Controller
@ConditionalOnProperty(value = "feature.onelogin.enabled", havingValue = "true")
public class SuperAdminController {
    private final DepartmentService departmentService;
    private final RoleService roleService;
    private final OneLoginUserService oneLoginUserService;

    @GetMapping("/super-admin-dashboard") ResponseEntity<SuperAdminDashboardPageDto> superAdminDashboard(
            final HttpServletRequest httpRequest,
            final Pageable pagination,
            @RequestParam(value = "departments", required = false, name = "departments") Integer[] departmentIds,
            @RequestParam(value = "roles", required = false, name = "roles") Integer[] roleIds,
            @RequestParam(value = "searchTerm", required = false) @Max(255) String searchTerm,
            @RequestParam(value = "clearAllFilters", required = false) boolean clearAllFilters) {
        if (!roleService.isSuperAdmin(httpRequest)) throw new ForbiddenException();

        final List<DepartmentDto> allDepartments = departmentService.getAllDepartments();
        final List<RoleDto> allRoles = roleService.getAllRoles();
        final Page<User> users;
        if (clearAllFilters) {
            users = oneLoginUserService.getPaginatedUsers(pagination, "", Collections.emptyList(),  Collections.emptyList());
        } else {
            users = oneLoginUserService.getPaginatedUsers(pagination, searchTerm, List.of(departmentIds), List.of(roleIds));
        }

        return ResponseEntity.ok(SuperAdminDashboardPageDto.builder()
                .departments(allDepartments)
                .roles(allRoles)
                .users(users.stream().map(UserDto::new).toList())
                .userCount(users.getTotalElements())
                .build());
    }
}