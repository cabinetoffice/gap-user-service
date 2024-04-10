package gov.cabinetoffice.gapuserservice.scheduler;

import gov.cabinetoffice.gapuserservice.enums.SpotlightOAuthAuditEvent;
import gov.cabinetoffice.gapuserservice.enums.SpotlightOAuthAuditStatus;
import gov.cabinetoffice.gapuserservice.model.SpotlightOAuthAudit;
import gov.cabinetoffice.gapuserservice.service.SpotlightService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SpotlightOAuthRefreshSchedulerTest {

    @Mock
    private SpotlightService spotlightService;

    @InjectMocks
    private SpotlightOAuthRefreshScheduler spotlightOAuthRefreshScheduler;

    @Test
    void spotlightOAuthTokenRefreshSchedule_Success() {
        ArgumentCaptor<SpotlightOAuthAudit> auditCaptor = ArgumentCaptor.forClass(SpotlightOAuthAudit.class);

        spotlightOAuthRefreshScheduler.spotlightOAuthTokenRefreshSchedule();

        verify(spotlightService).refreshToken();
        verify(spotlightService, times(2)).saveAudit(auditCaptor.capture());

        List<SpotlightOAuthAudit> capturedAudits = auditCaptor.getAllValues();
        assertEquals(2, capturedAudits.size());

        SpotlightOAuthAudit requestAudit = capturedAudits.get(0);
        Assertions.assertEquals(SpotlightOAuthAuditEvent.REFRESH, requestAudit.getEvent());
        Assertions.assertEquals(SpotlightOAuthAuditStatus.REQUEST, requestAudit.getStatus());

        SpotlightOAuthAudit successAudit = capturedAudits.get(1);
        Assertions.assertEquals(SpotlightOAuthAuditEvent.REFRESH, successAudit.getEvent());
        Assertions.assertEquals(SpotlightOAuthAuditStatus.SUCCESS, successAudit.getStatus());
    }

    @Test
    void spotlightOAuthTokenRefreshSchedule_Failure() {
        ArgumentCaptor<SpotlightOAuthAudit> auditCaptor = ArgumentCaptor.forClass(SpotlightOAuthAudit.class);
        doThrow(new RuntimeException()).when(spotlightService).refreshToken();

        spotlightOAuthRefreshScheduler.spotlightOAuthTokenRefreshSchedule();

        verify(spotlightService).refreshToken();
        verify(spotlightService, times(2)).saveAudit(auditCaptor.capture());

        List<SpotlightOAuthAudit> capturedAudits = auditCaptor.getAllValues();
        assertEquals(2, capturedAudits.size());

        SpotlightOAuthAudit requestAudit = capturedAudits.get(0);
        Assertions.assertEquals(SpotlightOAuthAuditEvent.REFRESH, requestAudit.getEvent());
        Assertions.assertEquals(SpotlightOAuthAuditStatus.REQUEST, requestAudit.getStatus());

        SpotlightOAuthAudit successAudit = capturedAudits.get(1);
        Assertions.assertEquals(SpotlightOAuthAuditEvent.REFRESH, successAudit.getEvent());
        Assertions.assertEquals(SpotlightOAuthAuditStatus.FAILURE, successAudit.getStatus());
    }
}
