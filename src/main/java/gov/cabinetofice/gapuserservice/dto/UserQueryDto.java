package gov.cabinetofice.gapuserservice.dto;

import gov.cabinetofice.gapuserservice.util.UserQueryCondition;

import java.util.List;

public record UserQueryDto (List<Integer> departmentIds, List<Integer> roleIds, String email) {
    public UserQueryCondition getCondition() {
        final boolean hasDepartment = !departmentIds.isEmpty();
        final boolean hasRole = !roleIds.isEmpty();
        final boolean hasEmail = email != null && !email.isBlank();
        return new UserQueryCondition(hasDepartment, hasRole, hasEmail);
    }
}

