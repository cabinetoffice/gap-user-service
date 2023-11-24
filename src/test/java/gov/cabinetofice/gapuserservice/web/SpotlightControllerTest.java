package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.enums.SpotlightOAuthAuditEvent;
import gov.cabinetofice.gapuserservice.enums.SpotlightOAuthAuditStatus;
import gov.cabinetofice.gapuserservice.exceptions.ForbiddenException;
import gov.cabinetofice.gapuserservice.exceptions.InvalidRequestException;
import gov.cabinetofice.gapuserservice.model.SpotlightOAuthAudit;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.RoleService;
import gov.cabinetofice.gapuserservice.service.SpotlightService;
import gov.cabinetofice.gapuserservice.service.jwt.impl.CustomJwtServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SpotlightControllerTest {

    @Mock
    private SpotlightService spotlightService;

    @Mock
    private RoleService roleService;

    @Mock
    private CustomJwtServiceImpl jwtService;

    private String ADMIN_BASE_URL = "http//:localhost:3000/adminBaseUrl";
    private String AUTHORIZE_URL = "http//:localhost:3000/authorize";
    private SpotlightController SpotlightController;


    @BeforeEach
    void setUp() {
        SpotlightController = new SpotlightController(spotlightService, roleService, jwtService);
        ReflectionTestUtils.setField(SpotlightController, "adminBaseUrl", ADMIN_BASE_URL);
    }

    @Nested
    class AuthorizeTest {


        @Test
        void shouldThrowExceptionIfNotSuperAdmin() throws Exception {
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            when(roleService.isSuperAdmin(httpRequest)).thenReturn(false);

            Exception exception = assertThrows(ForbiddenException.class, () -> {
                SpotlightController.authorize(httpRequest);
            });
        }

        @Test
        void shouldThrowExceptionIfNoUser() throws Exception {
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            when(roleService.isSuperAdmin(httpRequest)).thenReturn(true );
            when(jwtService.getUserFromJwt(httpRequest)).thenReturn(Optional.empty());
            Exception exception = assertThrows(InvalidRequestException.class, () -> {
                SpotlightController.authorize(httpRequest);
            });

            assertEquals("Could not get user from jwt", exception.getMessage());
        }

        @Test
        void shouldReturnAuthoriseUrl() throws Exception {
            User mockUser = User.builder().build();
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);

            // Create an ArgumentCaptor for SpotlightOAuthAudit
            ArgumentCaptor<SpotlightOAuthAudit> auditCaptor = ArgumentCaptor.forClass(SpotlightOAuthAudit.class);

            when(roleService.isSuperAdmin(httpRequest)).thenReturn(true );
            when(jwtService.getUserFromJwt(httpRequest)).thenReturn(Optional.of(mockUser));
            when(spotlightService.getAuthorizeUrl()).thenReturn(AUTHORIZE_URL);

            RedirectView redirectView = SpotlightController.authorize(httpRequest);

            verify(spotlightService).saveAudit(auditCaptor.capture());
            SpotlightOAuthAudit capturedAudit = auditCaptor.getValue();
            assertEquals(mockUser, capturedAudit.getUser());
            assertEquals(SpotlightOAuthAuditEvent.AUTHORISE, capturedAudit.getEvent());
            assertEquals(SpotlightOAuthAuditStatus.REQUEST, capturedAudit.getStatus());
            assertEquals(redirectView.getUrl(), AUTHORIZE_URL);
        }
    }

    @Nested
    class CallbackTest {

        private String CODE = "code";
        private String STATE = "state";

        @Test
        void shouldThrowExceptionIfNotSuperAdmin() throws Exception {
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            when(roleService.isSuperAdmin(httpRequest)).thenReturn(false);

            Exception exception = assertThrows(ForbiddenException.class, () -> {
                SpotlightController.callback(CODE, STATE, httpRequest);
            });
        }

        @Test
        void shouldThrowExceptionIfNoUser() throws Exception {
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            when(roleService.isSuperAdmin(httpRequest)).thenReturn(true );
            when(jwtService.getUserFromJwt(httpRequest)).thenReturn(Optional.empty());
            Exception exception = assertThrows(InvalidRequestException.class, () -> {
                SpotlightController.callback(CODE, STATE, httpRequest);
            });

            assertEquals("Could not get user from jwt", exception.getMessage());
        }

        @Test
        void shouldReturnRedirectUrl() throws Exception {
            User mockUser = User.builder().build();
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);

            // Create an ArgumentCaptor for SpotlightOAuthAudit
            ArgumentCaptor<SpotlightOAuthAudit> auditCaptor = ArgumentCaptor.forClass(SpotlightOAuthAudit.class);

            when(roleService.isSuperAdmin(httpRequest)).thenReturn(true );
            when(jwtService.getUserFromJwt(httpRequest)).thenReturn(Optional.of(mockUser));


            RedirectView redirectView = SpotlightController.callback(CODE, STATE, httpRequest);

            verify(spotlightService).saveAudit(auditCaptor.capture());
            SpotlightOAuthAudit capturedAudit = auditCaptor.getValue();
            assertEquals(mockUser, capturedAudit.getUser());
            assertEquals(SpotlightOAuthAuditEvent.AUTHORISE, capturedAudit.getEvent());
            assertEquals(SpotlightOAuthAuditStatus.SUCCESS, capturedAudit.getStatus());
            assertEquals(redirectView.getUrl(), ADMIN_BASE_URL + "?redirectUrl=/super-admin-dashboard");
        }

        @Test
        void shouldThrowExceptionIfTokenExchangeFails() throws IOException {
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            User mockUser = User.builder().build();
            ArgumentCaptor<SpotlightOAuthAudit> auditCaptor = ArgumentCaptor.forClass(SpotlightOAuthAudit.class);

            when(roleService.isSuperAdmin(httpRequest)).thenReturn(true );
            when(jwtService.getUserFromJwt(httpRequest)).thenReturn(Optional.of(mockUser));

            // Configure spotlightService to throw an exception when exchangeAuthorizationToken is called
            doThrow(new IOException("Test Exception")).when(spotlightService).exchangeAuthorizationToken(anyString(), anyString());


            Exception exception = assertThrows(Exception.class, () -> {
                SpotlightController.callback(CODE, STATE, httpRequest);
            });

            assertEquals("Error exchanging Spotlight authorization token", exception.getMessage());
            verify(spotlightService).saveAudit(auditCaptor.capture());
            SpotlightOAuthAudit capturedAudit = auditCaptor.getValue();
            assertEquals(mockUser, capturedAudit.getUser());
            assertEquals(SpotlightOAuthAuditEvent.AUTHORISE, capturedAudit.getEvent());
            assertEquals(SpotlightOAuthAuditStatus.FAILURE, capturedAudit.getStatus());
        }
    }

}
