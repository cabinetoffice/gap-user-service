package gov.cabinetoffice.gapuserservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "token_blacklist", indexes = @Index(columnList = "jwt"))
public class BlacklistedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(length = 4000)
    private String jwt;

    @Column
    private LocalDateTime expiryDate;
}
