package gov.cabinetofice.gapuserservice.web;

import com.auth0.jwt.interfaces.DecodedJWT;
import gov.cabinetofice.gapuserservice.dto.JwtPayload;
import gov.cabinetofice.gapuserservice.dto.RoleDto;
import gov.cabinetofice.gapuserservice.dto.UserRolesJwtResponse;
import gov.cabinetofice.gapuserservice.exceptions.UnauthorizedException;
import gov.cabinetofice.gapuserservice.exceptions.UserNotFoundException;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.OneLoginService;
import gov.cabinetofice.gapuserservice.service.RoleService;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@RestController
public class RoleController {
    private final RoleService roleService;
    private final OneLoginService oneLoginService;
    private final CustomJwtServiceImpl jwtService;

    @GetMapping("/role")
    public ResponseEntity<List<RoleDto>> getAll() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/user/roles")
    public ResponseEntity<UserRolesJwtResponse> getUserRolesFromJwt(final @RequestHeader("Authorization") String jwtToken) {
        if (jwtToken.length() <= 0) {
            throw new UnauthorizedException("No JWT token provided");
        }

        final String normalisedJwt = jwtToken.split(" ")[1];
        final boolean isValid = jwtService.isTokenValid(normalisedJwt);
        final DecodedJWT decodedJwt = jwtService.decodedJwt(normalisedJwt);
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
