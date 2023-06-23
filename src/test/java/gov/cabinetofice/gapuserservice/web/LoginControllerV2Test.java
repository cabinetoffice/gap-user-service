package gov.cabinetofice.gapuserservice.web;

import gov.cabinetofice.gapuserservice.service.OneLoginService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginControllerV2Test {

    @InjectMocks
    private LoginControllerV2 loginController;

    @Mock
    private OneLoginService oneLoginService;


    @Test
    void shouldReturnUserInfoTest() {

        when(oneLoginService.createOneLoginJwt()).thenReturn("test-jwt");
        when(oneLoginService.getAuthToken("test-jwt", "1234")).thenReturn("test-auth-token");
        when(oneLoginService.getUserInfo("test-auth-token")).thenReturn("test-user-info");

        ResponseEntity<String> result = loginController.redirect("1234");

        verify(oneLoginService, times(1)).createOneLoginJwt();
        verify(oneLoginService, times(1)).getAuthToken("test-jwt", "1234");
        verify(oneLoginService, times(1)).getUserInfo("test-auth-token");

        Assertions.assertEquals("test-user-info", result.getBody());

    }

}
