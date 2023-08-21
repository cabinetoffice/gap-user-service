package gov.cabinetofice.gapuserservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtPayload {
    private String sub;
    private String roles;
    private String iss;
    private String aud;
    private int exp;
    private int iat;
    private String email;
    private String idToken;
}