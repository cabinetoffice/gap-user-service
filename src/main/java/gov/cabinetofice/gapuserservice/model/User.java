package gov.cabinetofice.gapuserservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "hashedemail")
    private String hashedEmail;

    @Column(name = "encryptedemail")
    private String encryptedEmail;

    @Column(name = "sub")
    private String sub;

    @Column(name = "dept_id")
    private String departmentId;

    @ManyToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST }, fetch = FetchType.LAZY)
    @JoinColumn(name = "id", nullable = false)
    @ToString.Exclude
    @JsonIgnoreProperties({ "hibernateLazyInitializer" })
    private List<Role> roles;

    @ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST }, fetch = FetchType.LAZY)
    @JoinColumn(name = "id")
    @ToString.Exclude
    @JsonIgnoreProperties({ "hibernateLazyInitializer" })
    private Department department;

    public void addRole(final Role role) {
        this.roles.add(role);
        role.addUser(this);
    }
}
