package gov.cabinetofice.gapuserservice.security;

import gov.cabinetofice.gapuserservice.config.DebugProperties;
import gov.cabinetofice.gapuserservice.config.JwtProperties;
import gov.cabinetofice.gapuserservice.exceptions.UnauthorizedException;
import gov.cabinetofice.gapuserservice.service.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/**
 * This class cannot be a Spring bean, otherwise Spring will automatically apply it to all
 * requests, regardless of whether they've been specifically ignored
 */
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {
    private final DebugProperties debugProperties;

    private final JwtProperties jwtProperties;

    private final JwtService customJwtServiceImpl;

    private final String profile;

    @Override
    protected void doFilterInternal(final @NonNull HttpServletRequest request,
                                    final @NonNull HttpServletResponse response,
                                    final @NonNull FilterChain chain)
            throws ServletException, IOException {
        if(Objects.equals(profile, "LOCAL") && debugProperties.isIgnoreJwt()) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    "Placeholder",
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

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

        if(!customJwtServiceImpl.isTokenValid(userServiceJwt.get().getValue())) {
            throw new UnauthorizedException("Token not valid");
        }

        //TODO set the Security context, so we can access user details in rest of app
        final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "Placeholder",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }

}
