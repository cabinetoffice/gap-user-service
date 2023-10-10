package gov.cabinetofice.gapuserservice.exceptions;

public class GenerateTokenFailedException extends RuntimeException{
    public GenerateTokenFailedException(String message){ super(message); }
}
