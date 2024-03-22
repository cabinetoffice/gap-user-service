package gov.cabinetofice.gapuserservice.config;

import gov.cabinetofice.gapuserservice.security.interceptors.AuthorizationHeaderInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RequiredArgsConstructor
@Configuration
public class ServiceToServiceInterceptor  implements WebMvcConfigurer {
    private final ServiceToServiceConfigProperties serviceToServiceConfigProperties;

    @Bean
    public AuthorizationHeaderInterceptor serviceToServiceHeaderInterceptor() {
        return new AuthorizationHeaderInterceptor(serviceToServiceConfigProperties.getSecret(), serviceToServiceConfigProperties.getPrivateKey());
    }
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(serviceToServiceHeaderInterceptor())
                .addPathPatterns(
                        "/user",
                        "/users/emails"
                )
                .order(Ordered.HIGHEST_PRECEDENCE);
    }
}
