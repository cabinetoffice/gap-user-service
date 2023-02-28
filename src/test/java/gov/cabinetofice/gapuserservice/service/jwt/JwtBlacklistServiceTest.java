package gov.cabinetofice.gapuserservice.service.jwt;

import gov.cabinetofice.gapuserservice.model.JwtBlacklist;
import gov.cabinetofice.gapuserservice.repository.JwtBlacklistRepository;
import gov.cabinetofice.gapuserservice.service.JwtBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
public class JwtBlacklistServiceTest {

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
        final ArgumentCaptor<JwtBlacklist> blacklistCaptor = ArgumentCaptor.forClass(JwtBlacklist.class);

        serviceUnderTest.addJwtToBlacklist(jwt);
        verify(jwtBlacklistRepository).save(blacklistCaptor.capture());

        final JwtBlacklist capturedBlacklist = blacklistCaptor.getValue();
        assertThat(capturedBlacklist.getJwt()).isEqualTo(jwt);
    }

    @Test
    void deleteExpiredJwts_DeletesJwt() {
        final Date date = Date.from(clock.instant());
        when(jwtBlacklistRepository.deleteByExpiryDateLessThan(date))
                .thenReturn(Long.valueOf(1));

        when(serviceUnderTest.deleteExpiredJwts())
                .thenReturn(Long.valueOf(1));

        final Long numberOfDeletedJwts =  serviceUnderTest.deleteExpiredJwts();

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
