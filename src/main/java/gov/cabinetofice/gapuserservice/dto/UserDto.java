package gov.cabinetofice.gapuserservice.dto;

import gov.cabinetofice.gapuserservice.model.Department;
import gov.cabinetofice.gapuserservice.model.Role;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class UserDto {
    private String gap_user_id;
    private String email;
    private String sub;
    private List<Role> roles;
    private Department department;
}
