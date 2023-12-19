package gov.cabinetofice.gapuserservice.scheduler;

import gov.cabinetofice.gapuserservice.service.JwtBlacklistService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void deleteExpiredJwts_Delete(){
        jwtBlacklistScheduler.deleteExpiredJwts();
        verify(jwtBlacklistService).deleteExpiredJwts();
    }
}
