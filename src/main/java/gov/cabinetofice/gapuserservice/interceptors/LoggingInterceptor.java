package gov.cabinetofice.gapuserservice.interceptors;


import gov.cabinetofice.gapuserservice.config.ApplicationConfigProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import static net.logstash.logback.argument.StructuredArguments.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingInterceptor implements HandlerInterceptor {

    @Value("${jwt.cookie-name}")
    private String userServiceCookieName;

    private final ApplicationConfigProperties configProperties;

    private String getLogMessage(String logMessage) {
        // In local dev display request/response info in message
        if (Objects.equals(this.configProperties.getProfile(), "LOCAL")) return "{}:\n\t{\n\t\t{}\n\t\t{}\n\t\t{}\n\t\t{}\n\t}";
        // In prod display simple message
        return logMessage;
    }

    private Map<String, ArrayList<String>> getHeadersFromRequest(HttpServletRequest request) {
        Function<String, Enumeration<String>> getHeaders = request::getHeaders;
        Function<Enumeration<String>, ArrayList<String>> list = Collections::list;

        return Collections.list(request.getHeaderNames())
                .stream()
                .filter(h -> !h.equals("cookie"))
                .collect(Collectors.toMap(h -> h, getHeaders.andThen(list)));
    }

    private Map<String, Collection<String>> getHeadersFromResponse(HttpServletResponse response) {
        return new HashSet<>(response.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(h -> h, response::getHeaders));
    }

    private List<String> removeJWTFromCookies(Cookie[] cookies) {
        return Arrays.stream(cookies)
                .filter(c -> !c.getName().equals(userServiceCookieName))
                .map(c -> c.getName() + "=" + c.getValue())
                .collect(Collectors.toList());
    }

    private List<String> getCookiesFromRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return new ArrayList<>();
        }
        return removeJWTFromCookies(cookies);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String event = "Incoming request";
        log.info(
            getLogMessage(event),
            value("event", event),
            keyValue("URL", request.getRequestURL()),
            keyValue("method", request.getMethod()),
            keyValue("headers", getHeadersFromRequest(request)),
            keyValue("cookies", getCookiesFromRequest(request))
        );
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           @Nullable ModelAndView modelAndView) {
        String event = "Outgoing response";
        log.info(
            getLogMessage(event),
            value("event", event),
            keyValue("requestURL", request.getRequestURL()),
            keyValue("requestMethod", request.getMethod()),
            keyValue("status", response.getStatus()),
            keyValue("headers", getHeadersFromResponse(response))
        );
    }
}