package gov.cabinetoffice.gapuserservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RoleEnum {
    SUPER_ADMIN(4),
    ADMIN(3),
    APPLICANT(2),
    FIND(1),
    TECHNICAL_SUPPORT(5),
    ;

    final int roleId;
}