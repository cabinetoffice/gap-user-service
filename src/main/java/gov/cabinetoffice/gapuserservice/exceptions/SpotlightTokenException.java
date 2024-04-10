package gov.cabinetoffice.gapuserservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class SpotlightTokenException extends RuntimeException {
    public SpotlightTokenException(String message) {
        super(message);
    }
}
