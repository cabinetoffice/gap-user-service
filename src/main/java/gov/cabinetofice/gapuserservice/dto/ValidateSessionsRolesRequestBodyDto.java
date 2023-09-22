package gov.cabinetofice.gapuserservice.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class ValidateSessionsRolesRequestBodyDto {
    private String emailAddress;
    private String roles;
}
