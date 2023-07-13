package gov.cabinetofice.gapuserservice.security;

import gov.cabinetofice.gapuserservice.config.DebugProperties;
import gov.cabinetofice.gapuserservice.config.JwtProperties;
import gov.cabinetofice.gapuserservice.service.jwt.JwtService;
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
                             final JwtService customJwtServiceImpl,
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
        return web -> web.ignoring().requestMatchers(
                "/webjars/**",
                "/register/**",
                "/health",
                "/login",
                "/v2/notice-page",
                "/v2/redirect-after-login",
                "/v2/login",
                "/v2/notice-page",
                "/is-user-logged-in",
                "/redirect-after-cola-login",
                "/error/**",
                "/.well-known/jwks.json",
                "/logout"
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
