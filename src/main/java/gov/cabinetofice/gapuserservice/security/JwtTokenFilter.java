package gov.cabinetofice.gapuserservice.security;

import gov.cabinetofice.gapuserservice.config.DebugProperties;
import gov.cabinetofice.gapuserservice.config.JwtProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
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

    private final String profile;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
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
        final Optional<Cookie> userServiceJwt = Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals(jwtProperties.getCookieName()))
                .findFirst();

        if (userServiceJwt.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        try {
            //TODO verify the token
        } catch (Exception e) {
            //TODO throw appropriate exception if token is invalid
        }

        //TODO set the Security context, so we can access user details in rest of app
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "Placeholder",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }

}
