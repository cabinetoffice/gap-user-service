package gov.cabinetofice.gapuserservice.util;

public class UserQueryCondition {

        private final boolean hasDepartment;
        private final boolean hasRole;
        private final boolean hasEmail;

        public UserQueryCondition(boolean hasDepartment, boolean hasRole, boolean hasEmail) {
            this.hasDepartment = hasDepartment;
            this.hasRole = hasRole;
            this.hasEmail = hasEmail;
        }
    }
