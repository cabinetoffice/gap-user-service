package gov.cabinetofice.gapuserservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SpadminDashboardPageDto {
    private List<RoleDto> roles;
    private List<DeptDto> depts;
    private List<UserDto> users;
}
