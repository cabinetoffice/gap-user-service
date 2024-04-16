package gov.cabinetoffice.gapuserservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "salts")
public class Salt {
    @Id
    @Column(name = "salt_id")
    private String saltId;

    @Column(name = "salt")
    private String saltValue;

    @Column(name = "created_at")
    private Date createdAt;

    @PrePersist
    void createdAt() {
        this.createdAt = new Date();
    }
}
