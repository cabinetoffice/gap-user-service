package gov.cabinetofice.gapuserservice.service.jwt;

import gov.cabinetofice.gapuserservice.model.Blacklist;
import gov.cabinetofice.gapuserservice.repository.BlacklistRepository;
import gov.cabinetofice.gapuserservice.service.BlacklistService;
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
public class BlacklistServiceTest {

    @Mock
    private BlacklistRepository blacklistRepository;

    private BlacklistService serviceUnderTest;

    private final String jwt = "a-jwt";

    private final String CHRISTMAS_2022_MIDDAY = "2022-12-25T12:00:00.00z";

    private final Clock clock = Clock.fixed(Instant.parse(CHRISTMAS_2022_MIDDAY), ZoneId.of("UTC"));


    @BeforeEach
    void setup() {
        serviceUnderTest = new BlacklistService(blacklistRepository);
    }

    @Test
    void addJwtToBlacklist_Save() {
        final ArgumentCaptor<Blacklist> blacklistCaptor = ArgumentCaptor.forClass(Blacklist.class);

        serviceUnderTest.addJwtToBlacklist(jwt);
        verify(blacklistRepository).save(blacklistCaptor.capture());

        final Blacklist capturedBlacklist = blacklistCaptor.getValue();
        assertThat(capturedBlacklist.getJwt()).isEqualTo(jwt);
    }

//    @Test
//    void deleteExpiredJwts_DeletesJwt() {
//        final Date date = Date.from(clock.instant());
//        when(blacklistRepository.deleteByExpiryDateLessThan(date))
//                .thenReturn(Long.valueOf(1));
//
//        when(serviceUnderTest.deleteExpiredJwts())
//                .thenReturn(Long.valueOf(1));
//
//        final Long deletedJwts =  serviceUnderTest.deleteExpiredJwts();
//
//        verify(serviceUnderTest).deleteExpiredJwts();
//        //verify(blacklistRepository).deleteByExpiryDateLessThan(Date.from(clock.instant()));
//        assertThat(deletedJwts).isEqualTo(Long.valueOf(1));
//    }

    @Test
    void isJwtInBlacklist_ReturnsTrue() {
        when(blacklistRepository.existsByJwtIs(jwt)).thenReturn(true);
        boolean result = serviceUnderTest.isJwtInBlacklist(jwt);
        assertTrue(result);
    }
}
