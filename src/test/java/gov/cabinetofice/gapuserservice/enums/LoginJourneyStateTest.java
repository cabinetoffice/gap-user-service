package gov.cabinetofice.gapuserservice.enums;

import gov.cabinetofice.gapuserservice.dto.OneLoginUserInfoDto;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.OneLoginService;
import org.junit.jupiter.api.Nested;
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

    @Nested
    class PrivacyPolicyPending {
        @Test
        void returnsSelf_whenNotAccepted() {

        }

        @Test
        void returnsMigratingUser_whenAccepted() {

        }

        @Test
        void redirect() {

        }
    }

    @Nested
    class MigratingUser {
        @Test
        void flagOn_HasColaSub_NotUserReady() {

        }

        @Test
        void flagOn_HasColaSub_UserReady() {

        }

        @Test
        void flagOn_NoColaSub_NotUserReady() {

        }

        @Test
        void flagOn_NoColaSub_UserReady() {

        }

        @Test
        void flagOff_HasColaSub_NotUserReady() {

        }

        @Test
        void flagOff_HasColaSub_UserReady() {

        }

        @Test
        void flagOff_NoColaSub_NotUserReady() {

        }

        @Test
        void flagOff_NoColaSub_UserReady() {

        }

        @Test
        void redirect_super_admin() {

        }

        @Test
        void redirect_admin() {

        }

        @Test
        void redirect_technical_support() {

        }

        @Test
        void redirect_applicant() {

        }

        @Test
        void redirect_find() {

        }
    }

    @Nested
    class UserMigratedAndReady {

        @Test
        void emailUnchanged() {

        }

        @Test
        void emailChanged() {

        }

        @Test
        void redirect_super_admin() {

        }

        @Test
        void redirect_admin() {

        }

        @Test
        void redirect_technical_support() {

        }

        @Test
        void redirect_applicant() {

        }

        @Test
        void redirect_find() {

        }
    }

    @Nested
    class MigratingFindEmails {

        @Test
        void nextState() {

        }

        @Test
        void emailRedirectPage() {

        }
    }

    @Nested
    class UserReady {
        @Test
        void flagOn_HasColaSub() {

        }

        @Test
        void flagOn_NoColaSub() {

        }

        @Test
        void flagOff_emailsChanged() {

        }

        @Test
        void flagOff_emailsUnchanged() {

        }


        @Test
        void redirect_super_admin() {

        }

        @Test
        void redirect_admin() {

        }

        @Test
        void redirect_technical_support() {

        }

        @Test
        void redirect_applicant() {

        }

        @Test
        void redirect_find() {

        }
    }
}
