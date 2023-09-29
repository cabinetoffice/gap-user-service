package gov.cabinetofice.gapuserservice.enums;

import gov.cabinetofice.gapuserservice.dto.OneLoginUserInfoDto;
import gov.cabinetofice.gapuserservice.model.Role;
import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.user.OneLoginUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class LoginJourneyStateTest {

    @Mock
    private OneLoginUserService oneLoginUserService = mock(OneLoginUserService.class);

    @Mock
    private Logger logger = mock(Logger.class);

    @BeforeEach
    void setUp() {
        doNothing().when(logger).info(anyString());
    }

    @Nested
    class PrivacyPolicyPending {

        private final LoginJourneyState state = LoginJourneyState.PRIVACY_POLICY_PENDING;
        @Test
        void returnsSelf_whenNotAccepted() {
            final User user = User.builder()
                    .sub("sub")
                    .loginJourneyState(LoginJourneyState.USER_READY)
                    .roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build()))
                    .emailAddress("email@example.com")
                    .build();
            final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                    .emailAddress("email@example.com")
                    .sub("sub")
                    .build();
            final NextStateArgs nextStateArgs = NextStateArgs.builder()
                    .oneLoginUserService(oneLoginUserService)
                    .user(user)
                    .jwt("jwt")
                    .logger(logger)
                    .hasAcceptedPrivacyPolicy(false)
                    .userInfo(oneLoginUserInfoDto)
                    .findAccountsMigrationEnabled("false")
                    .build();

            final LoginJourneyState nextState = state.nextState(nextStateArgs);

            verify(oneLoginUserService, times(0)).setUsersLoginJourneyState(any(User.class), any(LoginJourneyState.class));
            assertEquals(LoginJourneyState.PRIVACY_POLICY_PENDING, nextState);
        }

        @Test
        void returnsMigratingUser_whenAccepted() {
            final User user = User.builder()
                    .sub("sub")
                    .loginJourneyState(LoginJourneyState.USER_READY)
                    .roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build()))
                    .emailAddress("email@example.com")
                    .build();
            final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                    .emailAddress("email@example.com")
                    .sub("sub")
                    .build();
            final NextStateArgs nextStateArgs = NextStateArgs.builder()
                    .oneLoginUserService(oneLoginUserService)
                    .user(user)
                    .jwt("jwt")
                    .logger(logger)
                    .hasAcceptedPrivacyPolicy(true)
                    .userInfo(oneLoginUserInfoDto)
                    .findAccountsMigrationEnabled("false")
                    .build();

            final LoginJourneyState nextState = state.nextState(nextStateArgs);

            verify(oneLoginUserService, times(1)).setUsersLoginJourneyState(user, LoginJourneyState.MIGRATING_USER);
            assertEquals(LoginJourneyState.MIGRATING_USER, nextState);
        }

        @Test
        void redirect() {
            final LoginJourneyRedirect redirect = state.getLoginJourneyRedirect(null);
            assertEquals(LoginJourneyRedirect.PRIVACY_POLICY_PAGE, redirect);
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
