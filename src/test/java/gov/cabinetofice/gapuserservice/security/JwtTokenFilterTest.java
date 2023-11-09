package gov.cabinetofice.gapuserservice.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import gov.cabinetofice.gapuserservice.config.DebugProperties;
import gov.cabinetofice.gapuserservice.config.JwtProperties;
import gov.cabinetofice.gapuserservice.dto.JwtPayload;
import gov.cabinetofice.gapuserservice.exceptions.UnauthorizedException;
import gov.cabinetofice.gapuserservice.service.jwt.JwtService;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenFilterTest {

    private JwtTokenFilter jwtTokenFilter;
    private @Mock DebugProperties debugProperties;
    private @Mock JwtProperties jwtProperties;
    private @Mock CustomJwtServiceImpl customJwtServiceImpl;
    private @Mock HttpServletRequest request;
    private @Mock HttpServletResponse response;
    private @Mock FilterChain chain;

    @BeforeEach
    void setup() {
        jwtTokenFilter = new JwtTokenFilter(debugProperties, jwtProperties, customJwtServiceImpl, "PROD");
    }

    @Test
    void Authenticates_when_TokenIsValid() throws ServletException, IOException {
        final SecurityContext securityContext = mock(SecurityContext.class);
        final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "Placeholder",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

        when(jwtProperties.getCookieName()).thenReturn("customJwt");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("customJwt", "value")});
        when(customJwtServiceImpl.isTokenValid("value")).thenReturn(true);
        final DecodedJWT decodedJwt = mock(DecodedJWT.class);
        final JwtPayload payload = mock(JwtPayload.class);
        when(customJwtServiceImpl.decodedJwt("value")).thenReturn(decodedJwt);
        when(customJwtServiceImpl.decodeTheTokenPayloadInAReadableFormat(decodedJwt)).thenReturn(payload);
        when(payload.getRoles()).thenReturn("[USER]");

        try (MockedStatic<SecurityContextHolder> staticSecurityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            staticSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            jwtTokenFilter.doFilterInternal(request, response, chain);

            verify(chain, times(1)).doFilter(request, response);
            verify(securityContext, times(1)).setAuthentication(authentication);
        }
    }

    @Test
    void Authenticates_when_ProfileIsLocal_and_DebugPropertiesIgnoresJwt() throws ServletException, IOException {
        final SecurityContext securityContext = mock(SecurityContext.class);
        final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "Placeholder",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        jwtTokenFilter = new JwtTokenFilter(debugProperties, jwtProperties, customJwtServiceImpl, "LOCAL");

        when(debugProperties.isIgnoreJwt()).thenReturn(true);

        try (MockedStatic<SecurityContextHolder> staticSecurityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            staticSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            jwtTokenFilter.doFilterInternal(request, response, chain);

            verify(chain, times(1)).doFilter(request, response);
            verify(securityContext, times(1)).setAuthentication(authentication);
        }
    }

    @Test
    void DoesNotAuthenticate_when_ProfileIsLocal_and_DebugPropertiesIgnoresJwtIsFalse_and_NoCookies() throws ServletException, IOException {
        final SecurityContext securityContext = mock(SecurityContext.class);

        jwtTokenFilter = new JwtTokenFilter(debugProperties, jwtProperties, customJwtServiceImpl, "LOCAL");
        when(debugProperties.isIgnoreJwt()).thenReturn(false);
        when(request.getCookies()).thenReturn(new Cookie[]{});

        try (MockedStatic<SecurityContextHolder> staticSecurityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            staticSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            jwtTokenFilter.doFilterInternal(request, response, chain);

            verify(chain, times(1)).doFilter(request, response);
            verify(securityContext, times(0)).setAuthentication(any());
        }
    }

    @Test
    void DoesNotAuthenticate_when_ProfileIsNotLocal_and_DebugPropertiesIgnoresJwt_and_NoCookies() throws ServletException, IOException {
        final SecurityContext securityContext = mock(SecurityContext.class);

        when(request.getCookies()).thenReturn(new Cookie[]{});

        try (MockedStatic<SecurityContextHolder> staticSecurityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            staticSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            jwtTokenFilter.doFilterInternal(request, response, chain);

            verify(chain, times(1)).doFilter(request, response);
            verify(securityContext, times(0)).setAuthentication(any());
        }
    }

    @Test
    void DoesNotAuthenticate_when_NoCookies() throws ServletException, IOException {
        final SecurityContext securityContext = mock(SecurityContext.class);

        when(request.getCookies()).thenReturn(new Cookie[]{});

        try (MockedStatic<SecurityContextHolder> staticSecurityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            staticSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            jwtTokenFilter.doFilterInternal(request, response, chain);

            verify(chain, times(1)).doFilter(request, response);
            verify(securityContext, times(0)).setAuthentication(any());
        }
    }

    @Test
    void DoesNotAuthenticate_when_JwtInvalid() {
        final SecurityContext securityContext = mock(SecurityContext.class);

        when(jwtProperties.getCookieName()).thenReturn("customJwt");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("customJwt", "value")});
        when(customJwtServiceImpl.isTokenValid("value")).thenReturn(false);

        final DecodedJWT decodedJwt = mock(DecodedJWT.class);
        final JwtPayload payload = mock(JwtPayload.class);
        when(customJwtServiceImpl.decodedJwt("value")).thenReturn(decodedJwt);
        when(customJwtServiceImpl.decodeTheTokenPayloadInAReadableFormat(decodedJwt)).thenReturn(payload);
        when(payload.getRoles()).thenReturn("[USER]");

        try (MockedStatic<SecurityContextHolder> staticSecurityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            staticSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            final UnauthorizedException error = assertThrows(UnauthorizedException.class, () -> jwtTokenFilter.doFilterInternal(request, response, chain));
            assertThat(error.getMessage()).isEqualTo("Token not valid");
            verify(securityContext, times(0)).setAuthentication(any());
        }
    }
}
