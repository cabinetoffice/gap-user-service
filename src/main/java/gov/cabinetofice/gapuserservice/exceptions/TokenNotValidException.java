package gov.cabinetofice.gapuserservice.exceptions;

public class TokenNotValidException extends RuntimeException {
    public TokenNotValidException(String message) {
        super(message);
    }
}
