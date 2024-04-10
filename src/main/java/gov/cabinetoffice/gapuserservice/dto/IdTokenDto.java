package gov.cabinetoffice.gapuserservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdTokenDto {
    private String sub;
    private String at_hash;
    private String iss;
    private String aud;
    private long exp;
    private long iat;
    private String vot;
    private String vtm;
    private String sid;
    private String nonce;
}