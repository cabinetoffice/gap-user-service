package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.DepartmentDto;
import gov.cabinetofice.gapuserservice.dto.RoleDto;
import gov.cabinetofice.gapuserservice.dto.SuperAdminDashboardPageDto;
import gov.cabinetofice.gapuserservice.dto.UserDto;
import gov.cabinetofice.gapuserservice.exceptions.ForbiddenException;
import gov.cabinetofice.gapuserservice.service.DepartmentService;
import gov.cabinetofice.gapuserservice.service.RoleService;
import gov.cabinetofice.gapuserservice.service.user.OneLoginUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;


@RequiredArgsConstructor
@Controller
@ConditionalOnProperty(value = "feature.onelogin.enabled", havingValue = "true")
public class SuperAdminController {
    private final DepartmentService departmentService;
    private final RoleService roleService;
    private final OneLoginUserService oneLoginUserService;

    @GetMapping("/super-admin-dashboard")
    public ResponseEntity<SuperAdminDashboardPageDto> superAdminDashboard(final HttpServletRequest httpRequest, final Pageable pagination) {
        if (roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }

        List<DepartmentDto> departments = departmentService.getAllDepartments();
        List<RoleDto> roles = roleService.getAllRoles();
        List<UserDto> users = oneLoginUserService.getPaginatedUsers(pagination);
        long userCount = oneLoginUserService.getUserCount();
        
        return ResponseEntity.ok(SuperAdminDashboardPageDto.builder()
                .departments(departments)
                .roles(roles)
                .users(users)
                .userCount(userCount)
                .build());
    }
}