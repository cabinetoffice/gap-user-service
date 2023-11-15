package gov.cabinetofice.gapuserservice.interceptors;

import gov.cabinetofice.gapuserservice.util.LoggingUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import static net.logstash.logback.argument.StructuredArguments.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingInterceptor implements HandlerInterceptor {

    private final LoggingUtils loggingUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getRequestURL().toString().endsWith("/health")) return true;
        String event = "Incoming request";
        log.info(
                loggingUtils.getJsonLogMessage(event, 7),
                value("event", event),
                keyValue("URL", request.getRequestURL()),
                keyValue("query", request.getQueryString()),
                keyValue("correlationId", MDC.get("CorrelationId")),
                keyValue("method", request.getMethod()),
                keyValue("headers", LoggingUtils.getHeadersFromRequest(request)),
                keyValue("cookies", loggingUtils.getCookiesFromRequest(request))
        );
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           @Nullable ModelAndView modelAndView) {
        if (request.getRequestURL().toString().endsWith("/health")) return;
        String event = "Outgoing response";
        log.info(
                loggingUtils.getJsonLogMessage(event, 7),
                value("event", event),
                keyValue("requestURL", request.getRequestURL()),
                keyValue("requestQuery", request.getQueryString()),
                keyValue("correlationId", MDC.get("CorrelationId")),
                keyValue("requestMethod", request.getMethod()),
                keyValue("status", response.getStatus()),
                keyValue("headers", LoggingUtils.getHeadersFromResponse(response))
        );
    }
}