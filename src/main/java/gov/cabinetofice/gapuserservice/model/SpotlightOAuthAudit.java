package gov.cabinetofice.gapuserservice.model;

import gov.cabinetofice.gapuserservice.enums.SpotlightOAuthAuditType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "spotlight_oauth_audit")
public class SpotlightOAuthAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private SpotlightOAuthAuditType type;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "gap_user_id")
    @ToString.Exclude
    private User user;

    @Column(name = "timestamp")
    private Date timestamp;

    @PrePersist
    void createdAt() {
        this.timestamp = new Date();
    }
}

