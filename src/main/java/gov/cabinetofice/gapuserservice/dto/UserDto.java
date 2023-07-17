package gov.cabinetofice.gapuserservice.dto;

import gov.cabinetofice.gapuserservice.model.Department;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class UserDto {
    private String gapUserId;
    private String emailAddress;
    private String sub;
    private List<RoleDto> roles;
    private Department department;
}
