package gov.cabinetofice.gapuserservice.enums;

import gov.cabinetofice.gapuserservice.dto.OneLoginUserInfoDto;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.OneLoginService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class LoginJourneyStateTest {
    @Test
    public void testNextStateWithEmailChange() {
        NextStateArgs nextStateArgs = mock(NextStateArgs.class);
        final User user = User.builder()
                .sub("sub")
                .loginJourneyState(LoginJourneyState.USER_READY)
                .roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build()))
                .emailAddress("old@example.com")
                .build();

        final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                .emailAddress("new@example.com")
                .sub("sub")
                .build();

        OneLoginService oneLoginService = mock(OneLoginService.class);

        when(nextStateArgs.user()).thenReturn(user);
        when(nextStateArgs.userInfo()).thenReturn(oneLoginUserInfoDto);
        when(nextStateArgs.oneLoginService()).thenReturn(oneLoginService);

        LoginJourneyState currentState = LoginJourneyState.USER_READY;
        LoginJourneyState nextState = currentState.nextState(nextStateArgs);

        verify(oneLoginService).setUsersEmail(user, "new@example.com");
        verify(oneLoginService).setUsersLoginJourneyState(user, LoginJourneyState.MIGRATING_FIND_EMAILS);
        assertEquals(LoginJourneyState.MIGRATING_FIND_EMAILS, nextState);
    }

    @Test
    public void testNextStateWithEmailNoChange() {
        NextStateArgs nextStateArgs = mock(NextStateArgs.class);
        final User user = User.builder()
                .sub("sub")
                .loginJourneyState(LoginJourneyState.USER_READY)
                .roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build()))
                .emailAddress("example@example.com")
                .build();

        final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                .emailAddress("example@example.com")
                .sub("sub")
                .build();

        OneLoginService oneLoginService = mock(OneLoginService.class);

        when(nextStateArgs.user()).thenReturn(user);
        when(nextStateArgs.userInfo()).thenReturn(oneLoginUserInfoDto);
        when(nextStateArgs.oneLoginService()).thenReturn(oneLoginService);

        LoginJourneyState currentState = LoginJourneyState.USER_READY;
        LoginJourneyState nextState = currentState.nextState(nextStateArgs);

        verify(oneLoginService, never()).setUsersEmail(any(User.class), anyString());
        verify(oneLoginService, never()).setUsersLoginJourneyState(any(User.class), any(LoginJourneyState.class));
        assertEquals(currentState, nextState);
    }
}
