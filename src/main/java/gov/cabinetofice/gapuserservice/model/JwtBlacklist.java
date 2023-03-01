package gov.cabinetofice.gapuserservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table
public class JwtBlacklist {
    @Id
    @GeneratedValue
    private int id;

    @Column
    private String jwt;

    @Column
    private LocalDateTime expiryDate;
}
