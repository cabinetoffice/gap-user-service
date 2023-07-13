package gov.cabinetofice.gapuserservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OneLoginUserInfoDto {
    private String emailAddress;
    private String sub;
    private List<RoleDto> roles;
}
