package gov.cabinetofice.gapuserservice.dto;

import gov.cabinetofice.gapuserservice.model.Department;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.User;
import jakarta.annotation.Nullable;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class UserDto {
    private String gapUserId;
    private String emailAddress;
    private String sub;
    private List<Role> roles;
    private Role role;
    private Department department;
    @Nullable
    private Instant created;

    public UserDto(final User user) {
        this.gapUserId = user.getGapUserId().toString();
        this.emailAddress = user.getEmailAddress();
        this.sub = user.getSub();
        this.department = user.getDepartment();
        this.roles = user.getRoles();
        this.role = user.getHighestRole();
        this.created = user.getCreated();
    }
}
