package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.*;
import gov.cabinetofice.gapuserservice.exceptions.ForbiddenException;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.DepartmentService;
import gov.cabinetofice.gapuserservice.service.RoleService;
import gov.cabinetofice.gapuserservice.service.user.OneLoginUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@RequiredArgsConstructor
@Controller
@ConditionalOnProperty(value = "feature.onelogin.enabled", havingValue = "true")
@Validated
public class SuperAdminController {
    private final DepartmentService departmentService;
    private final RoleService roleService;
    private final OneLoginUserService oneLoginUserService;

    @GetMapping("/super-admin-dashboard")
    ResponseEntity<SuperAdminDashboardPageDto> superAdminDashboard(
            final HttpServletRequest httpRequest,
            final Pageable pagination,
            @RequestParam(value = "departments", name = "departments") Integer[] departmentIds,
            @RequestParam(value = "roles", name = "roles") Integer[] roleIds,
            @RequestParam(value = "searchTerm") @Size(max = 255) String searchTerm) {
        if (!roleService.isSuperAdmin(httpRequest)) throw new ForbiddenException();

        final UserQueryDto userRequestDto = new UserQueryDto(List.of(departmentIds), List.of(roleIds), searchTerm);

        final List<DepartmentDto> allDepartments = departmentService.getAllDepartments();
        final List<RoleDto> allRoles = roleService.getAllRoles();
        final Page<User> users = oneLoginUserService.getPaginatedUsers(pagination, userRequestDto);

        return ResponseEntity.ok(SuperAdminDashboardPageDto.builder()
                .departments(allDepartments)
                .roles(allRoles)
                .users(users.stream().map(UserDto::new).toList())
                .userCount(users.getTotalElements())
                .build());
    }
}