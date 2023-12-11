package gov.cabinetofice.gapuserservice.dto;

import gov.cabinetofice.gapuserservice.model.Department;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.User;
import jakarta.annotation.Nullable;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class UserAndRelationsDto {
    private String gapUserId;
    private String emailAddress;
    private String sub;

    private String colaSub;
    private List<Role> roles;
    private Role role;
    private Department department;
    @Nullable
    private Instant created;

    public UserAndRelationsDto(final User user) {
        this.gapUserId = user.getGapUserId().toString();
        this.emailAddress = user.getEmailAddress();
        this.sub = user.getSub();
        this.department = user.hasDepartment() ? user.getDepartment() : null;
        this.roles = user.getRoles();
        this.role = user.getHighestRole();
        this.created = user.getCreated();
        this.colaSub = String.valueOf(user.getColaSub());
    }

}
