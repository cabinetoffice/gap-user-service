package gov.cabinetofice.gapuserservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class UserDto {
    private String gap_user_id;
    private String emailAddress;
    private String sub;
    private List<RoleDto> roles;
}