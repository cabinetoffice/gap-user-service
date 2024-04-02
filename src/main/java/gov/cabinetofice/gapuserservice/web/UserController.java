package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.annotations.ServiceToServiceHeaderValidation;
import gov.cabinetofice.gapuserservice.dto.*;
import gov.cabinetofice.gapuserservice.exceptions.ForbiddenException;
import gov.cabinetofice.gapuserservice.exceptions.InvalidRequestException;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.DepartmentService;
import gov.cabinetofice.gapuserservice.service.RoleService;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import gov.cabinetofice.gapuserservice.service.user.OneLoginUserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static gov.cabinetofice.gapuserservice.util.HelperUtils.getCustomJwtCookieFromRequest;


@RequiredArgsConstructor
@Controller
@ConditionalOnProperty(value = "feature.onelogin.enabled", havingValue = "true")
public class UserController {

    private static final String NO_USER = "Could not get user from jwt";
    private final OneLoginUserService oneLoginUserService;
    private final DepartmentService departmentService;
    private final RoleService roleService;
    private final CustomJwtServiceImpl jwtService;
    @Value("${jwt.cookie-name}")
    public String userServiceCookieName;

    @GetMapping("/userFromJwt")
    public ResponseEntity<UserAndRelationsDto> getUserFromJwt(HttpServletRequest httpRequest) {
        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }
        Optional<User> user = jwtService.getUserFromJwt(httpRequest);
        if (user.isEmpty()) {
            throw new InvalidRequestException(NO_USER);
        }

        return ResponseEntity.ok(new UserAndRelationsDto(user.get()));
    }

    @GetMapping("/isSuperAdmin")
    public ResponseEntity<String> isSuperAdmin(HttpServletRequest httpRequest) {
        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }
        return ResponseEntity.ok("Success");
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<UserAndRelationsDto> getUserById(HttpServletRequest httpRequest, @PathVariable("id") Integer id) {
        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }

        return ResponseEntity.ok(new UserAndRelationsDto(oneLoginUserService.getUserById(id)));
    }

    @GetMapping("/user")
    @ServiceToServiceHeaderValidation        // authenticate request from other services
    public ResponseEntity<UserAndRelationsDto> getUserByUserSub(@RequestParam("userSub") String userSub) {
        return ResponseEntity.ok(new UserAndRelationsDto(oneLoginUserService.getUserByUserSub(userSub)));
    }

    @GetMapping("/user/{userSub}/email")
    public ResponseEntity<String> getEmailFromSub(HttpServletRequest httpRequest, @PathVariable("userSub") String userSub) {
        return ResponseEntity.ok(oneLoginUserService.getUserBySub(userSub).getEmailAddress());
    }

    @PatchMapping("/user/{userId}/department")
    public ResponseEntity<User> updateDepartment(HttpServletRequest httpRequest, @PathVariable("userId") Integer userId,
                                                   @Validated @RequestBody ChangeDepartmentDto changeDepartmentDto) {

        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }

        if (oneLoginUserService.isUserApplicantAndFindOnly(oneLoginUserService.getUserById(userId))) {
            throw new InvalidRequestException("Users with find and applicant roles cannot be assigned a department");
        }

        if(changeDepartmentDto == null) return ResponseEntity.ok().build();
        final Cookie customJWTCookie = getCustomJwtCookieFromRequest(httpRequest, userServiceCookieName);
        User user = oneLoginUserService.updateDepartment(userId, changeDepartmentDto.getDepartmentId(), customJWTCookie.getValue());
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
    public ResponseEntity<String> updateRoles(HttpServletRequest httpRequest,
                                              @RequestBody() UpdateUserRolesRequestDto updateUserRolesRequestDto,
                                              @PathVariable("id") Integer id) {
        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }

        final Cookie customJWTCookie = getCustomJwtCookieFromRequest(httpRequest, userServiceCookieName);

        boolean isARequestToBlockUser = updateUserRolesRequestDto.newUserRoles().isEmpty();
        Optional<User> user = jwtService.getUserFromJwt(httpRequest);

        if (user.isEmpty()) {
            throw new InvalidRequestException(NO_USER);
        }
        if (isARequestToBlockUser && id.equals(user.get().getGapUserId())) {
            throw new UnsupportedOperationException("You can't block yourself");
        }

        oneLoginUserService.updateRoles(id, updateUserRolesRequestDto, customJWTCookie.getValue());
        return ResponseEntity.ok("success");
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity<String> deleteUser(HttpServletRequest httpRequest, @PathVariable("id") Integer id) {
        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }
        final Cookie customJWTCookie = getCustomJwtCookieFromRequest(httpRequest, userServiceCookieName);
        Optional<User> user = jwtService.getUserFromJwt(httpRequest);
        if (user.isEmpty()) {
            throw new InvalidRequestException(NO_USER);
        }
        if (user.get().getGapUserId().equals(id)) {
            throw new UnsupportedOperationException("You can't delete yourself");
        }

        oneLoginUserService.deleteUser(id, customJWTCookie.getValue());
        return ResponseEntity.ok("success");

    }

    @PostMapping("/users/emails")
    @ServiceToServiceHeaderValidation        // authenticate request from other services
    public ResponseEntity<List<UserEmailDto>> getUserEmailsBySubs(@RequestBody() List<String> subs) {
        return ResponseEntity.ok(oneLoginUserService.getUserEmailsBySubs(subs));
    }

    @PostMapping("/user-emails-from-subs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserEmailDto>> getUserEmailsFromSubs(
                @RequestBody UserSubsRequestDto req){
        return ResponseEntity.ok(oneLoginUserService.getUserEmailsBySubs(req.userSubs()));
    }


    @GetMapping("/user/email/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable("email") String email, @RequestParam Optional<String> role) {
        return ResponseEntity.ok(
                role.map(s -> new UserDto(oneLoginUserService.getUserByEmailAndRole(email, s)))
                        .orElseGet(() -> new UserDto(oneLoginUserService.getUserByEmail(email)))
        );
    }
}

