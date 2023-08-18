package gov.cabinetofice.gapuserservice.dto;

import javax.crypto.SecretKey;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class EncryptedResponseDto {
    private SecretKey privateKey;
    private String encryptedText;
}
