package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.dto.ChangeDepartmentPageDto;
import gov.cabinetofice.gapuserservice.dto.DepartmentDto;
import gov.cabinetofice.gapuserservice.dto.UserDto;
import gov.cabinetofice.gapuserservice.dto.UserEmailDto;
import gov.cabinetofice.gapuserservice.exceptions.ForbiddenException;
import gov.cabinetofice.gapuserservice.exceptions.InvalidRequestException;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.DepartmentService;
import gov.cabinetofice.gapuserservice.service.RoleService;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import gov.cabinetofice.gapuserservice.service.SecretAuthService;
import gov.cabinetofice.gapuserservice.service.user.OneLoginUserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static gov.cabinetofice.gapuserservice.util.HelperUtils.getCustomJwtCookieFromRequest;


@RequiredArgsConstructor
@Controller
@ConditionalOnProperty(value = "feature.onelogin.enabled", havingValue = "true")
public class UserController {

    private final OneLoginUserService oneLoginUserService;
    private final DepartmentService departmentService;
    private final RoleService roleService;
    private final CustomJwtServiceImpl jwtService;
    private final SecretAuthService secretAuthService;
    private static final String NO_USER = "Could not get user from jwt";

    @Value("${jwt.cookie-name}")
    public String userServiceCookieName;

    @GetMapping("/userFromJwt")
    public ResponseEntity<UserDto> getUserFromJwt(HttpServletRequest httpRequest) {
        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }
        Optional<User> user = jwtService.getUserFromJwt(httpRequest);
        if(user.isEmpty()){
            throw new InvalidRequestException(NO_USER);
        }

        return ResponseEntity.ok(new UserDto(user.get()));
    }

    @GetMapping("/isSuperAdmin")
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

    @GetMapping("/user")
    public ResponseEntity<UserDto> getUserByUserSub(@RequestParam("userSub") String userSub,
                                                    @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        // authenticate request from lambda function
        secretAuthService.authenticateSecret(authHeader);
        return ResponseEntity.ok(new UserDto(oneLoginUserService.getUserByUserSub(userSub)));
    }

    @PatchMapping("/user/{userId}/department")
    public ResponseEntity<User> updateDepartment(HttpServletRequest httpRequest, @PathVariable("userId") Integer userId,
                                                   @RequestParam(value = "departmentId", required = false) Integer departmentId) {

        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }

        if(oneLoginUserService.isUserApplicantAndFindOnly(oneLoginUserService.getUserById(userId))) {
            throw new InvalidRequestException("Users with find and applicant roles cannot be assigned a department");
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

        boolean isARequestToBlockUser = roleIds.isEmpty();
        Optional<User> user = jwtService.getUserFromJwt(httpRequest);

        if(user.isEmpty()){
            throw new InvalidRequestException(NO_USER);
        }
        if (isARequestToBlockUser && id.equals(user.get().getGapUserId())){
            throw new UnsupportedOperationException("You can't block yourself");
        }

        oneLoginUserService.updateRoles(id, roleIds);
        return ResponseEntity.ok("success");
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity<String> deleteUser(HttpServletRequest httpRequest, @PathVariable("id") Integer id) {
        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }
        final Cookie customJWTCookie = getCustomJwtCookieFromRequest(httpRequest, userServiceCookieName);
        Optional<User> user = jwtService.getUserFromJwt(httpRequest);
        if(user.isEmpty()){
            throw new InvalidRequestException(NO_USER);
        }
        if(user.get().getGapUserId().equals(id)) {
            throw new UnsupportedOperationException("You can't delete yourself");
        }

        oneLoginUserService.deleteUser(id, customJWTCookie.getValue());
        return ResponseEntity.ok("success");

    }

    @PostMapping("/users/emails")
    public ResponseEntity<List<UserEmailDto>> getUserEmailsBySubs(@RequestBody() List<String> subs, @RequestHeader(HttpHeaders.AUTHORIZATION) byte[] authHeader) {
        secretAuthService.authenticateEncryptedSecret(authHeader);
        return ResponseEntity.ok(oneLoginUserService.getUserEmailsBySubs(subs));
    }
}

