package gov.cabinetofice.gapuserservice.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class CorrelationIdInterceptor implements HandlerInterceptor {

    public static final String TCO_CORRELATION_ID = "tco-correlation-id";
    public static final String CORRELATION_ID = "CorrelationId";

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request,
                             @NotNull HttpServletResponse response,
                             @NotNull Object handler) {
        String correlationId =
                request.getHeader(TCO_CORRELATION_ID) != null ?
                        request.getHeader(TCO_CORRELATION_ID) :
                        getCorrelationId();
        MDC.put(CORRELATION_ID, correlationId);
        return true;
    }

    @Override
    public void postHandle(@NotNull HttpServletRequest request,
                           @NotNull HttpServletResponse response,
                           @NotNull Object handler,
                           @Nullable ModelAndView modelAndView) {
        response.addHeader(TCO_CORRELATION_ID, MDC.get(CORRELATION_ID));
    }

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request,
                                @NotNull HttpServletResponse response,
                                @NotNull Object handler,
                                Exception ex) {
        MDC.remove(CORRELATION_ID);
    }

    private String getCorrelationId() {
        return UUID.randomUUID().toString();
    }
}