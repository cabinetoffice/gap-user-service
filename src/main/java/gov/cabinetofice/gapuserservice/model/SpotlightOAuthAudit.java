package gov.cabinetofice.gapuserservice.model;

import gov.cabinetofice.gapuserservice.enums.SpotlightOAuthAuditEvent;
import gov.cabinetofice.gapuserservice.enums.SpotlightOAuthAuditStatus;
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
@Table(name = "spotlight_oauth_audit")
public class SpotlightOAuthAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "event")
    @Enumerated(EnumType.STRING)
    private SpotlightOAuthAuditEvent event;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private SpotlightOAuthAuditStatus status;

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

