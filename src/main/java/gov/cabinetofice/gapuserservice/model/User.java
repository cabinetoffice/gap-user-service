package gov.cabinetofice.gapuserservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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
    private String emailAddress;

    @Column(name = "sub")
    private String sub;

    @ManyToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST }, fetch = FetchType.LAZY, mappedBy = "users")
    @ToString.Exclude
    @JsonIgnoreProperties({ "hibernateLazyInitializer" })
    @Builder.Default
    private List<Role> roles = new ArrayList<>();

    @ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST }, fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_id")
    @ToString.Exclude
    @JsonIgnoreProperties({ "hibernateLazyInitializer" })
    private Department department;

    public void addRole(final Role role) {
        this.roles.add(role);
        role.addUser(this);
    }

    public boolean hasSub() {
        return this.sub != null;
    }

    public boolean hasEmail() {
        return this.emailAddress != null;
    }

    public boolean isApplicant() {
        return !isAdmin() && this.roles.stream().anyMatch((role) -> role.getName().equals(RoleEnum.APPLICANT));
    }

    public boolean isAdmin() {
        return this.roles.stream().anyMatch((role) -> role.getName().equals(RoleEnum.ADMIN) || role.getName().equals(RoleEnum.SUPER_ADMIN));
    }

    public boolean isSuperAdmin() {
        return this.roles.stream().anyMatch((role) -> role.getName().equals(RoleEnum.SUPER_ADMIN));
    }

    public User removeAllRoles() {
        for (Role role : this.roles) {
            role.removeUser(this);
        }
        this.roles.clear();
        return this;
    }
}
