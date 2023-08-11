package gov.cabinetofice.gapuserservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateNonceRedirectCookieDto {
    private String nonce;
    private String state;
    private String redirectUrl;
}
