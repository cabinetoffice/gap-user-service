package gov.cabinetoffice.gapuserservice.dto;

import lombok.Builder;

@Builder
public record UpdateFundingOrgDto(String sub, String email, String departmentName) {
}
