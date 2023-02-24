package gov.cabinetofice.gapuserservice.model;

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
@Table(name = "token_blacklist", indexes = @Index(columnList = "token"))
public class BlacklistedToken {

    @Id
    @GeneratedValue
    private int id;

    @Column(length = 4000)
    private String token;

    @Column
    private LocalDateTime expiry;
}
