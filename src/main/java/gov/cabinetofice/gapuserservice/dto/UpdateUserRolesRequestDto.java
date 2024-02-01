package gov.cabinetofice.gapuserservice.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record UpdateUserRolesRequestDto(Integer departmentId, List<Integer> newUserRoles) {
}
