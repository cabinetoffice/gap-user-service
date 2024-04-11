package gov.cabinetoffice.gapuserservice.web.controlleradvice;

import gov.cabinetoffice.gapuserservice.exceptions.NonceExpiredException;
import gov.cabinetoffice.gapuserservice.exceptions.UnauthorizedException;
import gov.cabinetoffice.gapuserservice.util.LoggingUtils;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

import static net.logstash.logback.argument.StructuredArguments.keyValue;
import static net.logstash.logback.argument.StructuredArguments.value;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class ControllerExceptionHandler {

    private static final String SESSION_EXPIRED = "session-expired";

    private final LoggingUtils loggingUtils;

    @ExceptionHandler(value = {
            HttpClientErrorException.class,
    })
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public ErrorMessage handle404s(Exception ex, WebRequest request) {
        return ErrorMessage.builder()
                .message(ex.getMessage())
                .build();
    }

    @ExceptionHandler(value = {
            NonceExpiredException.class,
    })
    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    public ModelAndView handleNonceExpiredException(Exception ex, WebRequest request) {
        return new ModelAndView(SESSION_EXPIRED);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponseBody handleValidationExceptions(MethodArgumentNotValidException ex) {
        return ErrorResponseBody.builder()
                .responseAccepted(Boolean.FALSE)
                .message("Validation failure")
                .errors(ex.getAllErrors()
                        .stream()
                        .map(e -> {
                            final String fieldName = e instanceof FieldError fieldError ? fieldError.getField() : e.getObjectName();
                            return Error.builder()
                                    .fieldName(fieldName)
                                    .errorMessage(e.getDefaultMessage())
                                    .build();
                        })
                        .toList()
                )
                .invalidData(ex.getBindingResult().getTarget())
                .build();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = {ConstraintViolationException.class})
    protected ErrorResponseBody handleConflict(ConstraintViolationException ex) {
        return ErrorResponseBody.builder()
                .responseAccepted(Boolean.FALSE)
                .message("Validation failure")
                .errors(ex.getConstraintViolations()
                        .stream()
                        .map(e -> {
                            final String fieldName = e instanceof FieldError fieldError ? fieldError.getField() : e.getPropertyPath().toString();
                            return Error.builder()
                                    .fieldName(fieldName)
                                    .errorMessage(e.getMessage())
                                    .build();
                        })
                        .toList()
                )
                .build();
    }

    @ExceptionHandler(value = {UnauthorizedException.class})
    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    public ErrorResponseBody handleUnauthorizedException(RuntimeException ex) {
        return ErrorResponseBody.builder()
                .responseAccepted(Boolean.FALSE)
                .message(ex.getMessage())
                .build();
    }

    @ExceptionHandler(Exception.class)
    public void handleAllExceptions(HttpServletRequest request, Throwable ex) throws Throwable {
        HttpStatus status = getStatus(request);
        String message = "Error processing request";
        log.error(
                loggingUtils.getJsonLogMessage(ex.getMessage(), 7),
                value("event", message),
                keyValue("status", status),
                keyValue("URL", request.getRequestURL()),
                keyValue("query", request.getQueryString()),
                keyValue("method", request.getMethod()),
                keyValue("headers", LoggingUtils.getHeadersFromRequest(request)),
                keyValue("cookies", loggingUtils.getCookiesFromRequest(request)),
                ex
        );
        throw ex;
    }

    private HttpStatus getStatus(HttpServletRequest request) {
        Object codeFromRequest = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Integer code = codeFromRequest != null ? (Integer) codeFromRequest : 500;
        HttpStatus status = HttpStatus.resolve(code);
        return (status != null) ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    }
}


