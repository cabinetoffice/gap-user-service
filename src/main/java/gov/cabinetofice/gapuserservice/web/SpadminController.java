package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.DeptDto;
import gov.cabinetofice.gapuserservice.dto.RoleDto;
import gov.cabinetofice.gapuserservice.dto.SpadminDashboardPageDto;
import gov.cabinetofice.gapuserservice.dto.UserDto;
import gov.cabinetofice.gapuserservice.service.DeptsService;
import gov.cabinetofice.gapuserservice.service.RoleService;
import gov.cabinetofice.gapuserservice.service.user.OneLoginUserService;
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
public class SpadminController {
    private final DeptsService deptsService;
    private final RoleService roleService;
    private final OneLoginUserService oneLoginUserService;

    @GetMapping("/spadmin-dashboard")
    public ResponseEntity<SpadminDashboardPageDto> spadminDashbaord(final Pageable pagination) {
        List<DeptDto> depts = deptsService.getAllDepts();
        List<RoleDto> roles = roleService.getAllRoles();
        List<UserDto> users = oneLoginUserService.getPaginatedUsers(pagination);
        
        return ResponseEntity.ok(SpadminDashboardPageDto.builder()
                .depts(depts)
                .roles(roles)
                .users(users)
                .build());
    }
}