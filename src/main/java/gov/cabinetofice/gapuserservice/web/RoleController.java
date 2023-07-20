package gov.cabinetofice.gapuserservice.web;

import com.auth0.jwt.interfaces.DecodedJWT;
import gov.cabinetofice.gapuserservice.dto.JwtPayload;
import gov.cabinetofice.gapuserservice.dto.RoleDto;
import gov.cabinetofice.gapuserservice.dto.UserRolesJwtResponse;
import gov.cabinetofice.gapuserservice.exceptions.ForbiddenException;
import gov.cabinetofice.gapuserservice.exceptions.UnauthorizedException;
import gov.cabinetofice.gapuserservice.exceptions.UserNotFoundException;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.OneLoginService;
import gov.cabinetofice.gapuserservice.service.RoleService;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@RestController
public class RoleController {
    private final RoleService roleService;
    private final OneLoginService oneLoginService;
    private final CustomJwtServiceImpl jwtService;

    @Value("${jwt.cookie-name}")
    public String userServiceCookieName;

    @GetMapping("/role")
    public ResponseEntity<List<RoleDto>> getAll(final HttpServletRequest httpRequest) {
        if (!roleService.isSuperAdmin(httpRequest)) {
            throw new ForbiddenException();
        }

        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/user/roles")
    public ResponseEntity<UserRolesJwtResponse> getUserRolesFromJwt(final HttpServletRequest request) {
        final Cookie customJWTCookie = WebUtils.getCookie(request, userServiceCookieName);
        if (customJWTCookie == null || customJWTCookie.getValue() == null) {
            throw new UnauthorizedException("No JWT token provided");
        }

        final boolean isValid = jwtService.isTokenValid(customJWTCookie.getValue());
        final DecodedJWT decodedJwt = jwtService.decodedJwt(customJWTCookie.getValue());
        final JwtPayload payload = jwtService.decodeTheTokenPayloadInAReadableFormat(decodedJwt);

        final Optional<User> optionalUser = oneLoginService.getUser(payload.getEmail(), payload.getSub());
        if (optionalUser.isEmpty()) {
            throw new UserNotFoundException("User not found");
        }
        final User user = optionalUser.get();

        final UserRolesJwtResponse response = UserRolesJwtResponse.builder()
                .isValid(isValid)
                .isSuperAdmin(user.isSuperAdmin())
                .isAdmin(user.isAdmin())
                .isApplicant(user.isApplicant())
                .build();

        return ResponseEntity.ok(response);
    }
}
