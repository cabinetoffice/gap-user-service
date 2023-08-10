package gov.cabinetofice.gapuserservice.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
@Table(name = "roles") //TODO database table names are usually singular
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    @Enumerated(EnumType.STRING)
    private RoleEnum name;

    @Column(name = "label")
    private String label;

    @Column(name = "description")
    private String description;

    @ManyToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST }, fetch = FetchType.LAZY)
    @JoinColumn(name = "id", nullable = false)
    @ToString.Exclude
    @JsonIgnoreProperties({ "hibernateLazyInitializer" })
    @JsonBackReference
    @Builder.Default
    private List<User> users = new ArrayList<>();

    public void addUser(User user) {
        this.users.add(user);
    }
    public void removeUser( User user) { this.users.remove(user); }
}

