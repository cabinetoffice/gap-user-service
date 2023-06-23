package gov.cabinetofice.gapuserservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class PrivateKeyParsingException extends RuntimeException {

    public PrivateKeyParsingException(String message) {
        super(message);
    }


}