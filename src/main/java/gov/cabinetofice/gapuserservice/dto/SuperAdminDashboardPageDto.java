package gov.cabinetofice.gapuserservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SuperAdminDashboardPageDto {
    private List<RoleDto> roles;
    private List<DepartmentDto> departments;
    private List<UserDto> users;
    private long userCount;
    private List<List<Integer>> previousFilterData;
}


