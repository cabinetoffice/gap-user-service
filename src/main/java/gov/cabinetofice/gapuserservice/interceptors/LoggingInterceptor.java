package gov.cabinetofice.gapuserservice.interceptors;


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Log4j2
public class LoggingInterceptor implements HandlerInterceptor {

    @Value("${jwt.cookie-name}")
    public String userServiceCookieName;

    public Map<String, ArrayList<String>> getHeadersFromRequest(HttpServletRequest request) {
        Function<String, Enumeration<String>> getHeaders = request::getHeaders;
        Function<Enumeration<String>, ArrayList<String>> list = Collections::list;

        return Collections.list(request.getHeaderNames())
                .stream()
                .filter(h -> !h.equals("cookies"))
                .collect(Collectors.toMap(h -> h, getHeaders.andThen(list)));
    }

    public Map<String, Collection<String>> getHeadersFromResponse(HttpServletResponse response) {
        return new HashSet<>(response.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(h -> h, response::getHeaders));
    }

    public List<String> removeJWTFromCookies(Cookie[] cookies) {
        return Arrays.stream(cookies)
                .filter(c -> !c.getName().equals(userServiceCookieName))
                .map(c -> c.getName() + "=" + c.getValue())
                .collect(Collectors.toList());
    }

    public List<String> getCookiesFromRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return new ArrayList<>();
        }
        return removeJWTFromCookies(cookies);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String jsonString = new JSONObject()
                .put("event", "incoming request")
                .put("URL", request.getRequestURL())
                .put("method", request.getMethod())
                .put("headers", new JSONObject(getHeadersFromRequest(request)))
                .put("cookies", getCookiesFromRequest(request))
                .toString();
        log.info(jsonString);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           @Nullable ModelAndView modelAndView) {
        String jsonString = new JSONObject()
                .put("event", "outgoing response")
                .put("requestURL", request.getRequestURL())
                .put("requestMethod", request.getMethod())
                .put("status",  response.getStatus())
                .put("headers", new JSONObject(getHeadersFromResponse(response)))
                .toString();
        log.info(jsonString);
    }
}