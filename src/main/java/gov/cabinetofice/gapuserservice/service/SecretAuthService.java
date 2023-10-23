package gov.cabinetofice.gapuserservice.service;

import gov.cabinetofice.gapuserservice.exceptions.UnauthorizedException;
import gov.cabinetofice.gapuserservice.service.encryption.AwsEncryptionServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SecretAuthService {

    @Value("${lambda.secret}")
    private String lambdaSecret;

    private AwsEncryptionServiceImpl awsEncryptionService;

    /**
     * Intended to authenticate requests coming from lambdas, which shouldn't pass through
     * the JWT auth process.
     * lambda controller methods
     * @param authHeader value taken from Authorization header
     */
    public void authenticateSecret(String authHeader) {
        if (!Objects.equals(lambdaSecret, authHeader)) {
            throw new UnauthorizedException("Secret key does not match");
        }
    }

    public void authenticateEncryptedSecret(byte[] authHeader) {
        String decryptedLambdaSecret = awsEncryptionService.decryptField(authHeader);
        if(!Objects.equals(lambdaSecret, decryptedLambdaSecret)) {
            throw new UnauthorizedException("Secret key does not match");
        }
    }

}
