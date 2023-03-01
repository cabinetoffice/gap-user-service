package gov.cabinetofice.gapuserservice.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import gov.cabinetofice.gapuserservice.model.BlacklistedToken;
import gov.cabinetofice.gapuserservice.repository.JwtBlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@RequiredArgsConstructor
@Service
public class JwtBlacklistService {
    private final JwtBlacklistRepository jwtBlacklistRepository;

    private final Clock clock;

    public void addJwtToBlacklist(final String jwt) {
        final DecodedJWT decodedToken = JWT.decode(jwt);
        final LocalDateTime expiry = decodedToken.getExpiresAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        final BlacklistedToken blacklist = BlacklistedToken.builder()
                .jwt(jwt)
                .expiryDate(expiry)
                .build();

        this.jwtBlacklistRepository.save(blacklist);
    }

    public boolean isJwtInBlacklist(final String jwt) {
        return jwtBlacklistRepository.existsByJwtIs(jwt);
    }

    public Long deleteExpiredJwts() {
        return this.jwtBlacklistRepository.deleteByExpiryDateLessThan(ZonedDateTime.now(clock).toLocalDateTime());
    }
}
