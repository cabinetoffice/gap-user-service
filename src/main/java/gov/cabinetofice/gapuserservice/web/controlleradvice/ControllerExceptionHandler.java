package gov.cabinetofice.gapuserservice.web.controlleradvice;

import gov.cabinetofice.gapuserservice.exceptions.UnauthorizedException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.WebRequest;

import java.time.Clock;

@RestControllerAdvice
@RequiredArgsConstructor
public class ControllerExceptionHandler {

    private final Clock clock;

    @ExceptionHandler(value = {
            HttpClientErrorException.class,
    })
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public ErrorMessage handle404s(Exception ex, WebRequest request) {
        return ErrorMessage.builder()
                .message(ex.getMessage())
                .build();
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
    @ExceptionHandler(value = { ConstraintViolationException.class })
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

    @ExceptionHandler(value = { UnauthorizedException.class })
    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    public ErrorResponseBody handleUnauthorizedException(RuntimeException ex) {
        return ErrorResponseBody.builder()
                .responseAccepted(Boolean.FALSE)
                .message(ex.getMessage())
                .build();
    }

}


