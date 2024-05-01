package gov.cabinetoffice.gapuserservice.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import gov.cabinetoffice.gapuserservice.dto.JwtPayload;
import gov.cabinetoffice.gapuserservice.dto.RoleDto;
import gov.cabinetoffice.gapuserservice.exceptions.UnauthorizedException;
import gov.cabinetoffice.gapuserservice.mappers.RoleMapper;
import gov.cabinetoffice.gapuserservice.model.RoleEnum;
import gov.cabinetoffice.gapuserservice.repository.RoleRepository;
import gov.cabinetoffice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    private final CustomJwtServiceImpl jwtService;

    @Value("${jwt.cookie-name}")
    public String userServiceCookieName;

    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(roleMapper::roleToRoleDto)
                .toList();
    }

    public boolean isSuperAdmin(final HttpServletRequest request) {
        final Cookie customJWTCookie = WebUtils.getCookie(request, userServiceCookieName);
        if (customJWTCookie == null || customJWTCookie.getValue() == null) {
            throw new UnauthorizedException("No JWT token provided");
        }

        final DecodedJWT decodedJwt = jwtService.decodedJwt(customJWTCookie.getValue());
        final JwtPayload payload = jwtService.decodeTheTokenPayloadInAReadableFormat(decodedJwt);

        return payload.getRoles().contains(RoleEnum.SUPER_ADMIN.toString());
    }
}
