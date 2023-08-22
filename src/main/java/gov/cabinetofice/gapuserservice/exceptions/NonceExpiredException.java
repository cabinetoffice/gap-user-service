package gov.cabinetofice.gapuserservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.I_AM_A_TEAPOT)
public class NonceExpiredException extends RuntimeException {

    public NonceExpiredException(String message) {
        super(message);
    }
}