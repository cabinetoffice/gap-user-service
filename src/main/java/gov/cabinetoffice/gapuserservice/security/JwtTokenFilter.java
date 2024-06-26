package gov.cabinetoffice.gapuserservice.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import gov.cabinetoffice.gapuserservice.config.DebugProperties;
import gov.cabinetoffice.gapuserservice.config.JwtProperties;
import gov.cabinetoffice.gapuserservice.dto.JwtPayload;
import gov.cabinetoffice.gapuserservice.exceptions.UnauthorizedException;
import gov.cabinetoffice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * This class cannot be a Spring bean, otherwise Spring will automatically apply it to all
 * requests, regardless of whether they've been specifically ignored
 */
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {
    private final DebugProperties debugProperties;

    private final JwtProperties jwtProperties;

    private final CustomJwtServiceImpl customJwtServiceImpl;
    private final String profile;

    @Override
    protected void doFilterInternal(final @NonNull HttpServletRequest request,
                                    final @NonNull HttpServletResponse response,
                                    final @NonNull FilterChain chain)
            throws ServletException, IOException {
        if (Objects.equals(profile, "LOCAL") && debugProperties.isIgnoreJwt()) {
            UsernamePasswordAuthenticationToken authentication = getUsernamePasswordAuthenticationToken();

            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
            return;
        }

        // Check if cookie exists. If not, return without setting authentication in the security context
        final Cookie[] cookies = request.getCookies();
        final Optional<Cookie> userServiceJwt = cookies == null ? Optional.empty() : Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(jwtProperties.getCookieName()))
                .findFirst();

        if (userServiceJwt.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        final DecodedJWT decodedJwt = customJwtServiceImpl.decodedJwt(userServiceJwt.get().getValue());
        final JwtPayload payload = customJwtServiceImpl.decodeTheTokenPayloadInAReadableFormat(decodedJwt);
        final List<String> roles = List.of(payload.getRoles()
                .replace("[", "")
                .replace("]", "")
                .replace(" ", "")
                .split(","));


        if (!customJwtServiceImpl.isTokenValid(userServiceJwt.get().getValue())) {
            throw new UnauthorizedException("Token not valid");
        }

        //Set the Security context, so we can access user details in rest of app
        final UsernamePasswordAuthenticationToken userAuthentication = new UsernamePasswordAuthenticationToken(
                "Placeholder",
                null,
                roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList());

        SecurityContextHolder.getContext().setAuthentication(userAuthentication);
        chain.doFilter(request, response);
    }

    private static UsernamePasswordAuthenticationToken getUsernamePasswordAuthenticationToken() {
        List<SimpleGrantedAuthority> roles = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_FIND"),
                new SimpleGrantedAuthority("ROLE_APPLICANT"),
                new SimpleGrantedAuthority("ROLE_TECH_SUPPORT_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")
                );
        return new UsernamePasswordAuthenticationToken(
                "Placeholder",
                null,
                roles);
    }

}
