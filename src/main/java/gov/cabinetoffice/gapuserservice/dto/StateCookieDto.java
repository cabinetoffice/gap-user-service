package gov.cabinetoffice.gapuserservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateCookieDto {
    private String state;
    private String redirectUrl;
    private String saltId;
}
