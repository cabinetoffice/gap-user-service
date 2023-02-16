package gov.cabinetofice.gapuserservice.exceptions;

public class JwtSignatureInvalid extends RuntimeException {

    public JwtSignatureInvalid(String message) {
        super(message);
    }

}