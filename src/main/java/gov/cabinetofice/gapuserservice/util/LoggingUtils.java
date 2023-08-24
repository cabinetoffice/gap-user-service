package gov.cabinetofice.gapuserservice.util;

import gov.cabinetofice.gapuserservice.config.ApplicationConfigProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@RequiredArgsConstructor
@Component
public class LoggingUtils {

    private final ApplicationConfigProperties configProperties;

    private String getInterpolationString (int noOfArguments) {
        return "\n\t{}".repeat(noOfArguments);
    }

    private String getInterpolationString (int noOfArguments, String prefix) {
        String pattern = prefix + "{}";
        return pattern.repeat(noOfArguments);
    }

    /**
     * Produces a log message formatted correctly for either prod or local dev, depending on the value of spring.profiles.active
     * in application.properties.
     *
     * Interpolated console output (using StructuredArguments) will take the form:
     *
     * message:
     *  firstKey=firstValue
     *  secondKey=secondValue
     *  etc
     *
     * @param message - message to prepend before interpolated arguments in console and add to "message" property in JSON
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
     *
     * Interpolated console output (using StructuredArguments) will take the form:
     *
     * message:
     *  {
     *      firstKey=firstValue
     *      secondKey=secondValue
     *      etc
     *  }
     *
     * @param message - message to prepend before interpolated arguments in console and add to "message" property in JSON
     * @param noOfArguments - number of interpolated arguments to expect
     * @return Locally: message followed by JSON-formatted template. In prod: message.
     */
    public String getJsonLogMessage(String message, int noOfArguments) {
        // In local dev display request/response info in message
        if (Objects.equals(this.configProperties.getProfile(), "LOCAL"))
            return "{}:\n\t{" + getInterpolationString(noOfArguments, "\n\t\t") + "\n\t}";
        // In prod display simple message
        return message;
    }
}
