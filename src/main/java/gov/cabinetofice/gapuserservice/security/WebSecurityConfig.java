package gov.cabinetofice.gapuserservice.security;


import gov.cabinetofice.gapuserservice.config.DebugProperties;
import gov.cabinetofice.gapuserservice.config.JwtProperties;
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

    private final DebugProperties debugProperties;

    private final JwtProperties jwtProperties;

    private final String profile;

    public WebSecurityConfig(DebugProperties debugProperties,
                             JwtProperties jwtProperties,
                             @Value("${spring.profiles.active:PROD}") String profile) {
        this.debugProperties = debugProperties;
        this.jwtProperties = jwtProperties;
        this.profile = profile;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        // specify any paths you don't want subject to JWT validation/authentication
        // TODO if not using swagger, remove relevant paths here or remove this comment
        return web -> web.ignoring().requestMatchers(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-resources/**",
                "/swagger-ui.html",
                "/webjars/**",
                "/health2",
                "/login");
    }

    @Bean
    public SecurityFilterChain filterChainPublic(HttpSecurity http) throws Exception {
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
                .addFilterBefore(new JwtTokenFilter(debugProperties, jwtProperties, profile), UsernamePasswordAuthenticationFilter.class);

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
