package gov.cabinetofice.gapuserservice.dto;

import lombok.Data;

@Data
public class ValidateSessionsRolesRequestBodyDto {
    private String emailAddress;
    private String roles;
}
