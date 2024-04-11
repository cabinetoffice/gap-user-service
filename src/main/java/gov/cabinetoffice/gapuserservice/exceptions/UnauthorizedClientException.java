package gov.cabinetoffice.gapuserservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedClientException extends RuntimeException {

    public UnauthorizedClientException() {
    }

    public UnauthorizedClientException(String message) {
        super(message);
    }

    public UnauthorizedClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnauthorizedClientException(Throwable cause) {
        super(cause);
    }

}