package gov.cabinetofice.gapuserservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import gov.cabinetofice.gapuserservice.enums.LoginJourneyState;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private Integer gapUserId;

    @Column(name = "email")
    private String emailAddress;

    @Column(name = "sub")
    private String sub;

    @Column(name = "cola_sub")
    private UUID colaSub;

    @Column(name = "login_journey_state")
    @Enumerated(EnumType.STRING)
    private LoginJourneyState loginJourneyState;

    @Column(name = "created")
    private Instant created;

    @ManyToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST }, fetch = FetchType.LAZY)
    @JoinTable(name = "roles_users",
            joinColumns = { @JoinColumn(name = "users_gap_user_id", referencedColumnName = "gap_user_id") },
            inverseJoinColumns = { @JoinColumn(name = "roles_id", referencedColumnName = "id") }
    )
    @ToString.Exclude
    @JsonManagedReference
    @Builder.Default
    private List<Role> roles = new ArrayList<>();

    @ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST }, fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_id")
    @ToString.Exclude
    @JsonManagedReference
    @JsonIgnoreProperties({ "hibernateLazyInitializer" })
    private Department department;

    @PrePersist
    @PreUpdate
    void created() {
        this.created = Instant.now();
    }

    public void addRole(final Role role) {
        this.roles.add(role);
    }

    public boolean hasSub() {
        return this.sub != null;
    }

    public boolean hasEmail() {
        return this.emailAddress != null;
    }

    public boolean hasDepartment() {
        return this.department != null;
    }

    public boolean hasColaSub() {
        return this.colaSub != null;
    }

    public boolean isApplicant() {
        return this.roles.stream().anyMatch((role) -> role.getName().equals(RoleEnum.APPLICANT) ||
                role.getName().equals(RoleEnum.ADMIN) ||
                role.getName().equals(RoleEnum.SUPER_ADMIN));
    }

    public boolean isAdmin() {
        return this.roles.stream().anyMatch(
                (role) -> role.getName().equals(RoleEnum.ADMIN) || role.getName().equals(RoleEnum.SUPER_ADMIN));
    }

    public boolean isSuperAdmin() {
        return this.roles.stream().anyMatch((role) -> role.getName().equals(RoleEnum.SUPER_ADMIN));
    }

    public boolean isTechnicalSupport() {
        return this.roles.stream().anyMatch(role -> role.getName().equals(RoleEnum.TECHNICAL_SUPPORT));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public Role getHighestRole() {
        if (isSuperAdmin())
            return this.roles.stream().filter(role -> role.getName().equals(RoleEnum.SUPER_ADMIN)).findFirst().get();
        if (isAdmin())
            return this.roles.stream().filter(role -> role.getName().equals(RoleEnum.ADMIN)).findFirst().get();
        if (isTechnicalSupport())
            return this.roles.stream().filter(role -> role.getName().equals(RoleEnum.TECHNICAL_SUPPORT)).findFirst()
                    .get();
        if (isApplicant())
            return this.roles.stream().filter(role -> role.getName().equals(RoleEnum.APPLICANT)).findFirst().get();
        return null;
    }

    public boolean hasAcceptedPrivacyPolicy() {
        return this.getLoginJourneyState() != LoginJourneyState.PRIVACY_POLICY_PENDING;
    }

    public void removeAllRoles() {
        this.roles.clear();
    }
}
