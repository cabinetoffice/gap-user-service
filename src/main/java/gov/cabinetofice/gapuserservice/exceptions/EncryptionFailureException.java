package gov.cabinetofice.gapuserservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class EncryptionFailureException extends RuntimeException {
    public EncryptionFailureException(String message) {
        super(message);
    }
}
