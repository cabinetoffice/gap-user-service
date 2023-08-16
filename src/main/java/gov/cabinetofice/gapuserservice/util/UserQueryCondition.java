package gov.cabinetofice.gapuserservice.util;

public record UserQueryCondition(boolean hasDepartment, boolean hasRole, boolean hasEmail){}
