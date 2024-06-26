package gov.cabinetoffice.gapuserservice.security;

import gov.cabinetoffice.gapuserservice.config.DebugProperties;
import gov.cabinetoffice.gapuserservice.config.JwtProperties;
import gov.cabinetoffice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    private final JwtTokenFilter jwtTokenFilter;

    public WebSecurityConfig(final DebugProperties debugProperties,
                             final JwtProperties jwtProperties,
                             final CustomJwtServiceImpl customJwtServiceImpl,
                             final @Value("${spring.profiles.active:PROD}") String profile) {
        this.jwtTokenFilter = new JwtTokenFilter(debugProperties, jwtProperties, customJwtServiceImpl, profile);
    }

    /**
     * Using WebSecurityCustomizer#ignoring triggers a warning at app start-up for each path ignored.
     * Unfortunately, its recommendation is not suitable for us, since we not only need authentication to be ignored
     * but also the JWT filter - permitAll via HttpSecurity#authorizeHttpRequests will still trigger the JwtTokenFilter
     * One alternative is to create two SecurityFilterChain beans, one for public paths and another for secured.
     * But the secured path then cannot use anyRequest(): you must specify every path you want authenticated.
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        // specify any paths you don't want subject to JWT validation/authentication
        //if you add any path used by other internal services (and we want to check the authorization header secret),
        // remember to add the path also in src/main/java/gov/cabinetoffice/gapuserservice/config/ServiceToServiceInterceptor.java
        return web -> web.ignoring().requestMatchers(
                "/webjars/**",
                "/register/**",
                "/health",
                "/login",
                "/v2/redirect-after-login",
                "/v2/login",
                "/v2/logout",
                "/v2/notice-page",
                "/v2/privacy-policy",
                "/v2/updated-email",
                "/is-user-logged-in",
                "/redirect-after-cola-login",
                "/error/**",
                "/v2/validateSessionsRoles",
                "/user",
                "/users/emails"
        );
    }

    @Bean
    public SecurityFilterChain filterChainPublic(final HttpSecurity http) throws Exception {
        // disable session creation by Spring Security, since auth will happen on every request
        http
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        // any requests to any path must be authenticated (unless specified in webSecurityCustomizer bean)
        http
                .authorizeHttpRequests()
                .anyRequest()
                .authenticated();

        // add JwtTokenFilter to the auth chain
        http
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);

        // disable a bunch of Spring Security default stuff we don't need
        http
                .formLogin().disable()
                .httpBasic().disable()
                .logout().disable()
                .csrf().disable();

        // handle exceptions when non-auth'd user hits an endpoint
        http
                .exceptionHandling()
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));

        return http.build();
    }

}
