package gov.cabinetoffice.gapuserservice.exceptions;

public class JwkNotValidTokenException extends RuntimeException {

    public JwkNotValidTokenException(String message) {
        super(message);
    }

}