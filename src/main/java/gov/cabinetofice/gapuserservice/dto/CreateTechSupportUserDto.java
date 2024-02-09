package gov.cabinetofice.gapuserservice.dto;

import lombok.Builder;

@Builder
public record CreateTechSupportUserDto(String userSub, String departmentName) {
}
