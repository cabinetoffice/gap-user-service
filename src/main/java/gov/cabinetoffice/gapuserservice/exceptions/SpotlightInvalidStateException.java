package gov.cabinetoffice.gapuserservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class SpotlightInvalidStateException extends RuntimeException {
    public SpotlightInvalidStateException(String message) {
        super(message);
    }
}
