package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.mappers.RoleMapper;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.user.OneLoginUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.LinkedHashMap;


@RequiredArgsConstructor
@Controller
@ConditionalOnProperty(value = "feature.onelogin.enabled", havingValue = "true")
public class UserController {

    private final OneLoginUserService oneLoginUserService;
    private final RoleMapper roleMapper;

    @Value("${super-admin-redirect-url}")
    private String redirectUrl;


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
    public RedirectView updateRoles(@RequestBody() Object body,
                                             @PathVariable("id") Integer id) {
        oneLoginUserService.updateRoles(id, ((LinkedHashMap) body).get("newUserRoles"));
        return new RedirectView(redirectUrl + "/edit-role/" + id);
    }
}