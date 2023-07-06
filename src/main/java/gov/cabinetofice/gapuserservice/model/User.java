package gov.cabinetofice.gapuserservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "gap_users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gap_user_id")
    private Integer id;

    @Column(name = "email")
    private String email;

    @Column(name = "sub")
    private String sub;

    @ManyToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST }, fetch = FetchType.EAGER, mappedBy = "users")
    @ToString.Exclude
    @JsonIgnoreProperties({ "hibernateLazyInitializer" })
    @Builder.Default
    private List<Role> roles = new ArrayList<>();

    @ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST }, fetch = FetchType.EAGER)
    @JoinColumn(name = "dept_id")
    @ToString.Exclude
    @JsonIgnoreProperties({ "hibernateLazyInitializer" })
    private Department department;

    public void addRole(final Role role) {
        this.roles.add(role);
        role.addUser(this);
    }

    public List<RoleEnum> getUsersRoles() {
        return this.roles.stream().map(Role::getName).collect(Collectors.toList());
    }

    public boolean hasSub() {
        return this.sub != null;
    }

    public boolean hasEmail() {
        return this.email != null;
    }

    public boolean isAnApplicant() {
        final List<RoleEnum> userRoles = getUsersRoles();
        return !isAnAdmin() && userRoles.stream().anyMatch((role) -> role.equals(RoleEnum.APPLICANT));
    }

    public boolean isAnAdmin() {
        final List<RoleEnum> userRoles = getUsersRoles();
        return userRoles.stream().anyMatch((role) -> role.equals(RoleEnum.ADMIN) || role.equals(RoleEnum.SUPER_ADMIN));
    }

    public boolean isASuperAdmin() {
        final List<RoleEnum> userRoles = getUsersRoles();
        return userRoles.stream().anyMatch((role) -> role.equals(RoleEnum.SUPER_ADMIN));
    }
}
