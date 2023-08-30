package gov.cabinetofice.gapuserservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OneLoginUserInfoDto {
    private String emailAddress;
    private String sub;
}
