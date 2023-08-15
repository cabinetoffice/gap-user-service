package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.ChangeDepartmentPageDto;
import gov.cabinetofice.gapuserservice.dto.DepartmentDto;
import gov.cabinetofice.gapuserservice.dto.UserDto;
import gov.cabinetofice.gapuserservice.exceptions.ForbiddenException;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.DepartmentService;
import gov.cabinetofice.gapuserservice.service.RoleService;
import gov.cabinetofice.gapuserservice.service.user.OneLoginUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequiredArgsConstructor
@Controller
@ConditionalOnProperty(value = "feature.onelogin.enabled", havingValue = "true")
public class UserController {

    private final OneLoginUserService oneLoginUserService;
    private final DepartmentService departmentService;
    private final RoleService roleService;

    @GetMapping("user")
    public ResponseEntity<String> isSuperAdmin(HttpServletRequest httpRequest) {
        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }
        return ResponseEntity.ok("Success");
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<UserDto> getUserById(HttpServletRequest httpRequest, @PathVariable("id") Integer id) {
        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }

        return ResponseEntity.ok(new UserDto(oneLoginUserService.getUserById(id)));
    }
    @PatchMapping("/user/{userId}/department")
    public ResponseEntity<User> updateDepartment(HttpServletRequest httpRequest, @PathVariable("userId") Integer userId,
                                                   @RequestParam(value = "departmentId", required = false) Integer departmentId) {
        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }

        if(departmentId == null) return ResponseEntity.ok().build();
        User user = oneLoginUserService.updateDepartment(userId, departmentId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/page/user/{userId}/change-department")
    public ResponseEntity<ChangeDepartmentPageDto> getChangeDepartmentPage(HttpServletRequest httpRequest, @PathVariable("userId") Integer userId) {
        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }

        final User user = oneLoginUserService.getUserById(userId);
        final List<DepartmentDto> departments = departmentService.getAllDepartments();
        return ResponseEntity.ok(ChangeDepartmentPageDto.builder()
                .departments(departments)
                .user(user)
                .build());
    }

    @PatchMapping("/user/{id}/role")
    public ResponseEntity<String> updateRoles(HttpServletRequest httpRequest, @RequestBody() List<Integer> roleIds,
                                              @PathVariable("id") Integer id) {
        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }

        oneLoginUserService.updateRoles(id, roleIds);
        return ResponseEntity.ok("success");
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity<String> deleteUser(HttpServletRequest httpRequest, @PathVariable("id") Integer id) {
        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }

        oneLoginUserService.deleteUser(id);
        return ResponseEntity.ok("success");
    }
}