package gov.cabinetofice.gapuserservice.service.jwt;

import com.auth0.jwt.JWT;
import gov.cabinetofice.gapuserservice.model.BlacklistedToken;
import gov.cabinetofice.gapuserservice.repository.JwtBlacklistRepository;
import gov.cabinetofice.gapuserservice.service.JwtBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.Date;

import static com.auth0.jwt.JWT.decode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtBlacklistServiceTest {

    @Mock
    private JwtBlacklistRepository jwtBlacklistRepository;

    private JwtBlacklistService serviceUnderTest;

    private final String jwt = "a-jwt";

    private final String CHRISTMAS_2022_MIDDAY = "2022-12-25T12:00:00.00z";

    private final Clock clock = Clock.fixed(Instant.parse(CHRISTMAS_2022_MIDDAY), ZoneId.of("UTC"));


    @BeforeEach
    void setup() {
        serviceUnderTest = new JwtBlacklistService(jwtBlacklistRepository, clock);
    }

    @Test
    void addJwtToBlacklist_Save() {
        final long now = ZonedDateTime.now(clock).toInstant().toEpochMilli();
        final TestDecodedJwt decodedToken = TestDecodedJwt.builder()
                .expiresAt(new Date(now))
                .build();

        try (MockedStatic<JWT> staticJwt = Mockito.mockStatic(JWT.class)) {
            staticJwt.when(() -> decode(jwt))
                    .thenReturn(decodedToken);

            final ArgumentCaptor<BlacklistedToken> blacklistCaptor = ArgumentCaptor.forClass(BlacklistedToken.class);

            serviceUnderTest.addJwtToBlacklist(jwt);
            verify(jwtBlacklistRepository).save(blacklistCaptor.capture());

            final BlacklistedToken capturedBlacklist = blacklistCaptor.getValue();
            assertThat(capturedBlacklist.getJwt()).isEqualTo(jwt);
        }
    }

    @Test
    void deleteExpiredJwts_DeletesJwt() {
        final LocalDateTime date = ZonedDateTime.now(clock).toLocalDateTime();
        when(jwtBlacklistRepository.deleteByExpiryDateLessThan(date))
                .thenReturn(Long.valueOf(1));

        when(serviceUnderTest.deleteExpiredJwts())
                .thenReturn(Long.valueOf(1));

        final Long numberOfDeletedJwts = serviceUnderTest.deleteExpiredJwts();

        verify(jwtBlacklistRepository).deleteByExpiryDateLessThan(date);
        assertThat(numberOfDeletedJwts).isEqualTo(Long.valueOf(1));
    }

    @Test
    void isJwtInBlacklist_ReturnsTrue() {
        when(jwtBlacklistRepository.existsByJwtIs(jwt)).thenReturn(true);
        boolean result = serviceUnderTest.isJwtInBlacklist(jwt);
        assertTrue(result);
    }
}
