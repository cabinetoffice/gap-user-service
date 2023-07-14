package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.mappers.RoleMapper;
import gov.cabinetofice.gapuserservice.model.User;
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
    private final RoleMapper roleMapper;

    @GetMapping("/user/{id}")
    public ResponseEntity<User> getUserById(@PathVariable("id") Integer id) {
       return ResponseEntity.ok(oneLoginUserService.getUserById(id));
   }

    @PatchMapping("/user/{id}/department")
    public ResponseEntity<User> updateDepartment(@PathVariable("id") Integer id,
                                                   @RequestParam("departmentId") Integer departmentId) {
        User user = oneLoginUserService.updateDepartment(id, departmentId);
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/user/{id}/role")
    public ResponseEntity<String> updateRoles(@RequestBody() List<Integer> roleIds,
                                @PathVariable("id") Integer id) {
        oneLoginUserService.updateRoles(id, roleIds);
        return ResponseEntity.ok("success");
    }
}