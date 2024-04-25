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
@Table(name = "spotlight_oauth_state")
public class SpotlightOAuthState {
    @Id
    @Column(name = "state_id")
    private Integer state_id;

    @Column(name = "state")
    private String state;

    @Column(name = "last_updated")
    private Date lastUpdatedAt;

    @PreUpdate
    void lastUpdatedAt() {
        this.lastUpdatedAt = new Date();
    }
}
