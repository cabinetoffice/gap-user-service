package gov.cabinetofice.gapuserservice.util;

import gov.cabinetofice.gapuserservice.config.ApplicationConfigProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class LoggingUtils {

    @Value("${jwt.cookie-name}")
    private String userServiceCookieName;

    private final ApplicationConfigProperties configProperties;

    private String getInterpolationString(int noOfArguments) {
        return "\n\t{}".repeat(noOfArguments);
    }

    private String getInterpolationString(int noOfArguments, String prefix) {
        String pattern = prefix + "{}";
        return pattern.repeat(noOfArguments);
    }

    /**
     * Produces a log message formatted correctly for either prod or local dev, depending on the value of spring.profiles.active
     * in application.properties.
     * <p>
     * Interpolated console output (using StructuredArguments) will take the form:
     * <p>
     * message:
     * firstKey=firstValue
     * secondKey=secondValue
     * etc
     *
     * @param message       - message to prepend before interpolated arguments in console and add to "message" property in JSON
     * @param noOfArguments - number of interpolated arguments to expect
     * @return Locally: message followed by template. In prod: message.
     */
    public String getLogMessage(String message, int noOfArguments) {
        if (Objects.equals(configProperties.getProfile(), "LOCAL")) {
            return message + getInterpolationString(noOfArguments);
        }
        return message;
    }

    /**
     * Produces a log message formatted correctly for either prod or local dev, depending on the value of spring.profiles.active
     * in application.properties. This will output a "JSON-style" log message in local dev, useful for logging things like requests.
     * <p>
     * Interpolated console output (using StructuredArguments) will take the form:
     * <p>
     * message:
     * {
     * firstKey=firstValue
     * secondKey=secondValue
     * etc
     * }
     *
     * @param message       - message to prepend before interpolated arguments in console and add to "message" property in JSON
     * @param noOfArguments - number of interpolated arguments to expect
     * @return Locally: message followed by JSON-formatted template. In prod: message.
     */
    public String getJsonLogMessage(String message, int noOfArguments) {
        // In local dev display request/response info in message
        if (Objects.equals(this.configProperties.getProfile(), "LOCAL"))
            return message + ":\n\t{" + getInterpolationString(noOfArguments, "\n\t\t") + "\n\t}";
        // In prod display simple message
        return message;
    }

    public static Map<String, ArrayList<String>> getHeadersFromRequest(HttpServletRequest request) {
        Function<String, Enumeration<String>> getHeaders = request::getHeaders;
        Function<Enumeration<String>, ArrayList<String>> list = Collections::list;

        return Collections.list(request.getHeaderNames())
                .stream()
                .filter(h -> !h.equals("cookie"))
                .collect(Collectors.toMap(h -> h, getHeaders.andThen(list)));
    }

    public static Map<String, Collection<String>> getHeadersFromResponse(HttpServletResponse response) {
        return new HashSet<>(response.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(h -> h, response::getHeaders));
    }

    private List<String> removeJWTFromCookies(Cookie[] cookies) {
        return Arrays.stream(cookies)
                .filter(c -> !c.getName().equals(userServiceCookieName))
                .map(c -> c.getName() + "=" + c.getValue())
                .toList();
    }

    public List<String> getCookiesFromRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return new ArrayList<>();
        }
        return removeJWTFromCookies(cookies);
    }
}
