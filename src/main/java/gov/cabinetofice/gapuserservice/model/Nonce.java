package gov.cabinetofice.gapuserservice.model;

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
@Table(name = "nonces")
public class Nonce {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nonce_id")
    private Integer nonceId;

    @Column(name = "nonce")
    private String nonceString;

    @Column(name = "created_at")
    private Date createdAt;

    @PrePersist
    void createdAt() {
        this.createdAt = new Date();
    }
}
