package gov.cabinetoffice.gapuserservice.scheduler;

import gov.cabinetoffice.gapuserservice.service.JwtBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtBlacklistSchedulerTest {

    @Mock
    private JwtBlacklistService jwtBlacklistService;

    private JwtBlacklistScheduler jwtBlacklistScheduler;

    @BeforeEach
    void setup() {
        jwtBlacklistScheduler = new JwtBlacklistScheduler(jwtBlacklistService);
    }

    @Test
    void deleteExpiredJwts_Delete() {
        jwtBlacklistScheduler.deleteExpiredJwts();
        verify(jwtBlacklistService).deleteExpiredJwts();
    }
}
