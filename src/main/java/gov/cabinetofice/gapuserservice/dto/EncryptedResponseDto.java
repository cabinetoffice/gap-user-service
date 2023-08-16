package gov.cabinetofice.gapuserservice.dto;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class EncryptedResponseDto {
    private byte [] IV;
    private SecretKey privateKey;
    private String encryptedText;
    private String signature;
    private KeyPair signingKeyPair;
    private String shaSum;
}
