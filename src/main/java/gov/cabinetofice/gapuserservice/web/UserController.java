package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.ChangeDepartmentPageDto;
import gov.cabinetofice.gapuserservice.dto.DepartmentDto;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.DepartmentService;
import gov.cabinetofice.gapuserservice.service.user.OneLoginUserService;
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

    @GetMapping("/user/{id}")
    public ResponseEntity<User> getUserById(@PathVariable("id") Integer id) {
       return ResponseEntity.ok(oneLoginUserService.getUserById(id));
   }

    @PatchMapping("/user/{userId}/department")
    public ResponseEntity<User> updateDepartment(@PathVariable("userId") Integer userId,
                                                 @RequestParam(value = "departmentId", required = false) Integer departmentId) {
        if(departmentId == null) return ResponseEntity.ok().build();
        User user = oneLoginUserService.updateDepartment(userId, departmentId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/page/user/{userId}/change-department")
    public ResponseEntity<ChangeDepartmentPageDto> getChangeDepartmentPage(@PathVariable("userId") Integer userId) {
        final User user = oneLoginUserService.getUserById(userId);
        final List<DepartmentDto> departments = departmentService.getAllDepartments();
        return ResponseEntity.ok(ChangeDepartmentPageDto.builder()
                .departments(departments)
                .user(user)
                .build());
    }

    @PatchMapping("/user/{id}/role")
    public ResponseEntity<String> updateRoles(@RequestBody() List<Integer> roleIds,
                                              @PathVariable("id") Integer id) {
        oneLoginUserService.updateRoles(id, roleIds);
        return ResponseEntity.ok("success");
    }
}