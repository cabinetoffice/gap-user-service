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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LoginJourneyStateTest {

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
        private final User user = User.builder()
                .sub("sub")
                .loginJourneyState(state)
                .roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build()))
                .emailAddress("email@example.com")
                .build();
        private final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                .emailAddress("email@example.com")
                .sub("sub")
                .build();

        @Test
        void returnsSelf_whenNotAccepted() {
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

            verify(oneLoginUserService, never()).setUsersLoginJourneyState(any(User.class), any(LoginJourneyState.class));
            assertEquals(LoginJourneyState.PRIVACY_POLICY_PENDING, nextState);
        }

        @Test
        void returnsMigratingUser_whenAccepted() {
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
            final LoginJourneyRedirect redirect = state.getLoginJourneyRedirect(null,null);
            assertEquals(LoginJourneyRedirect.PRIVACY_POLICY_PAGE, redirect);
        }
    }

    @Nested
    class MigratingUser {
        private final LoginJourneyState state = LoginJourneyState.MIGRATING_USER;
        private final User user = spy(User.builder()
                .sub("sub")
                .loginJourneyState(state)
                .roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build()))
                .emailAddress("email@example.com")
                .colaSub(UUID.randomUUID())
                .build());
        private final OneLoginUserInfoDto oneLoginUserInfoDto = OneLoginUserInfoDto.builder()
                .emailAddress("email@example.com")
                .sub("sub")
                .build();

        @BeforeEach
        void setup() {
            reset(user);
        }

        @Test
        void flagOn_HasColaSub_NotUserReady() {
            final NextStateArgs nextStateArgs = NextStateArgs.builder()
                    .oneLoginUserService(oneLoginUserService)
                    .user(user)
                    .jwt("jwt")
                    .logger(logger)
                    .hasAcceptedPrivacyPolicy(true)
                    .userInfo(oneLoginUserInfoDto)
                    .findAccountsMigrationEnabled("true")
                    .build();

            when(user.hasColaSub()).thenReturn(true);
            when(user.getLoginJourneyState()).thenReturn(LoginJourneyState.MIGRATING_USER);

            final LoginJourneyState nextState = state.nextState(nextStateArgs);

            verify(oneLoginUserService, times(1)).migrateFindUser(user, nextStateArgs.jwt());
            verify(oneLoginUserService, times(1)).migrateApplyUser(user, nextStateArgs.jwt());
            verify(oneLoginUserService, times(1)).setUsersLoginJourneyState(user, LoginJourneyState.USER_MIGRATED_AND_READY);
            assertEquals(LoginJourneyState.MIGRATING_USER, nextState);
        }

        @Test
        void flagOn_HasColaSub_UserReady() {
            final NextStateArgs nextStateArgs = NextStateArgs.builder()
                    .oneLoginUserService(oneLoginUserService)
                    .user(user)
                    .jwt("jwt")
                    .logger(logger)
                    .hasAcceptedPrivacyPolicy(true)
                    .userInfo(oneLoginUserInfoDto)
                    .findAccountsMigrationEnabled("true")
                    .build();

            when(user.hasColaSub()).thenReturn(true);
            when(user.getLoginJourneyState()).thenReturn(LoginJourneyState.USER_READY);

            final LoginJourneyState nextState = state.nextState(nextStateArgs);

            verify(oneLoginUserService, times(1)).migrateFindUser(user, nextStateArgs.jwt());
            verify(oneLoginUserService, never()).migrateApplyUser(user, nextStateArgs.jwt());
            verify(oneLoginUserService, times(1)).setUsersLoginJourneyState(user, LoginJourneyState.USER_MIGRATED_AND_READY);
            assertEquals(LoginJourneyState.MIGRATING_USER, nextState);
        }

        @Test
        void flagOn_NoColaSub_NotUserReady() {
            final NextStateArgs nextStateArgs = NextStateArgs.builder()
                    .oneLoginUserService(oneLoginUserService)
                    .user(user)
                    .jwt("jwt")
                    .logger(logger)
                    .hasAcceptedPrivacyPolicy(true)
                    .userInfo(oneLoginUserInfoDto)
                    .findAccountsMigrationEnabled("true")
                    .build();

            when(user.hasColaSub()).thenReturn(false);
            when(user.getLoginJourneyState()).thenReturn(LoginJourneyState.MIGRATING_USER);

            final LoginJourneyState nextState = state.nextState(nextStateArgs);

            verify(oneLoginUserService, times(1)).migrateFindUser(user, nextStateArgs.jwt());
            verify(oneLoginUserService, never()).migrateApplyUser(user, nextStateArgs.jwt());
            verify(oneLoginUserService, times(1)).setUsersLoginJourneyState(user, LoginJourneyState.USER_MIGRATED_AND_READY);
            assertEquals(LoginJourneyState.MIGRATING_USER, nextState);
        }

        @Test
        void flagOn_NoColaSub_UserReady() {
            final NextStateArgs nextStateArgs = NextStateArgs.builder()
                    .oneLoginUserService(oneLoginUserService)
                    .user(user)
                    .jwt("jwt")
                    .logger(logger)
                    .hasAcceptedPrivacyPolicy(true)
                    .userInfo(oneLoginUserInfoDto)
                    .findAccountsMigrationEnabled("true")
                    .build();

            when(user.hasColaSub()).thenReturn(false);
            when(user.getLoginJourneyState()).thenReturn(LoginJourneyState.USER_READY);

            final LoginJourneyState nextState = state.nextState(nextStateArgs);

            verify(oneLoginUserService, times(1)).migrateFindUser(user, nextStateArgs.jwt());
            verify(oneLoginUserService, never()).migrateApplyUser(user, nextStateArgs.jwt());
            verify(oneLoginUserService, times(1)).setUsersLoginJourneyState(user, LoginJourneyState.USER_MIGRATED_AND_READY);
            assertEquals(LoginJourneyState.MIGRATING_USER, nextState);
        }

        @Test
        void flagOff_HasColaSub_NotUserReady() {
            final NextStateArgs nextStateArgs = NextStateArgs.builder()
                    .oneLoginUserService(oneLoginUserService)
                    .user(user)
                    .jwt("jwt")
                    .logger(logger)
                    .hasAcceptedPrivacyPolicy(true)
                    .userInfo(oneLoginUserInfoDto)
                    .findAccountsMigrationEnabled("false")
                    .build();

            when(user.hasColaSub()).thenReturn(true);
            when(user.getLoginJourneyState()).thenReturn(LoginJourneyState.MIGRATING_USER);

            final LoginJourneyState nextState = state.nextState(nextStateArgs);

            verify(oneLoginUserService, never()).migrateFindUser(user, nextStateArgs.jwt());
            verify(oneLoginUserService, times(1)).migrateApplyUser(user, nextStateArgs.jwt());
            verify(oneLoginUserService, times(1)).setUsersLoginJourneyState(user, LoginJourneyState.USER_READY);
            assertEquals(LoginJourneyState.MIGRATING_USER, nextState);
        }

        @Test
        void flagOff_HasColaSub_UserReady() {
            final NextStateArgs nextStateArgs = NextStateArgs.builder()
                    .oneLoginUserService(oneLoginUserService)
                    .user(user)
                    .jwt("jwt")
                    .logger(logger)
                    .hasAcceptedPrivacyPolicy(true)
                    .userInfo(oneLoginUserInfoDto)
                    .findAccountsMigrationEnabled("false")
                    .build();

            when(user.hasColaSub()).thenReturn(true);
            when(user.getLoginJourneyState()).thenReturn(LoginJourneyState.USER_READY);

            final LoginJourneyState nextState = state.nextState(nextStateArgs);

            verify(oneLoginUserService, never()).migrateFindUser(user, nextStateArgs.jwt());
            verify(oneLoginUserService, never()).migrateApplyUser(user, nextStateArgs.jwt());
            verify(oneLoginUserService, times(1)).setUsersLoginJourneyState(user, LoginJourneyState.USER_READY);
            assertEquals(LoginJourneyState.MIGRATING_USER, nextState);
        }

        @Test
        void flagOff_NoColaSub_NotUserReady() {
            final NextStateArgs nextStateArgs = NextStateArgs.builder()
                    .oneLoginUserService(oneLoginUserService)
                    .user(user)
                    .jwt("jwt")
                    .logger(logger)
                    .hasAcceptedPrivacyPolicy(true)
                    .userInfo(oneLoginUserInfoDto)
                    .findAccountsMigrationEnabled("false")
                    .build();

            when(user.hasColaSub()).thenReturn(false);
            when(user.getLoginJourneyState()).thenReturn(LoginJourneyState.MIGRATING_USER);

            final LoginJourneyState nextState = state.nextState(nextStateArgs);

            verify(oneLoginUserService, never()).migrateFindUser(user, nextStateArgs.jwt());
            verify(oneLoginUserService, never()).migrateApplyUser(user, nextStateArgs.jwt());
            verify(oneLoginUserService, times(1)).setUsersLoginJourneyState(user, LoginJourneyState.USER_READY);
            assertEquals(LoginJourneyState.MIGRATING_USER, nextState);
        }

        @Test
        void flagOff_NoColaSub_UserReady() {
            final NextStateArgs nextStateArgs = NextStateArgs.builder()
                    .oneLoginUserService(oneLoginUserService)
                    .user(user)
                    .jwt("jwt")
                    .logger(logger)
                    .hasAcceptedPrivacyPolicy(true)
                    .userInfo(oneLoginUserInfoDto)
                    .findAccountsMigrationEnabled("false")
                    .build();

            when(user.hasColaSub()).thenReturn(false);
            when(user.getLoginJourneyState()).thenReturn(LoginJourneyState.USER_READY);

            final LoginJourneyState nextState = state.nextState(nextStateArgs);

            verify(oneLoginUserService, never()).migrateFindUser(user, nextStateArgs.jwt());
            verify(oneLoginUserService, never()).migrateApplyUser(user, nextStateArgs.jwt());
            verify(oneLoginUserService, times(1)).setUsersLoginJourneyState(user, LoginJourneyState.USER_READY);
            assertEquals(LoginJourneyState.MIGRATING_USER, nextState);
        }

        @Test
        void redirect_super_admin() {
            final LoginJourneyRedirect redirect = state.getLoginJourneyRedirect(RoleEnum.SUPER_ADMIN,null);
            assertEquals(LoginJourneyRedirect.SUPER_ADMIN_DASHBOARD, redirect);
        }

        @Test
        void redirect_admin() {
            final LoginJourneyRedirect redirect = state.getLoginJourneyRedirect(RoleEnum.ADMIN,null);
            assertEquals(LoginJourneyRedirect.ADMIN_MIGRATED, redirect);
        }

        @Test
        void redirect_technical_support() {
            final LoginJourneyRedirect redirect = state.getLoginJourneyRedirect(RoleEnum.TECHNICAL_SUPPORT,null);
            assertEquals(LoginJourneyRedirect.TECHNICAL_SUPPORT_DASHBOARD, redirect);
        }

        @Test
        void redirect_applicant() {
            final LoginJourneyRedirect redirect = state.getLoginJourneyRedirect(RoleEnum.APPLICANT,null);
            assertEquals(LoginJourneyRedirect.APPLICANT_MIGRATED, redirect);
        }

        @Test
        void redirect_find() {
            final LoginJourneyRedirect redirect = state.getLoginJourneyRedirect(RoleEnum.FIND,null);
            assertEquals(LoginJourneyRedirect.APPLICANT_MIGRATED, redirect);
        }
    }

    @Nested
    class UserMigratedAndReady {
        private final LoginJourneyState state = LoginJourneyState.USER_MIGRATED_AND_READY;
        private final User user = spy(User.builder()
                .sub("sub")
                .loginJourneyState(state)
                .roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build()))
                .emailAddress("email@example.com")
                .build());
        private final OneLoginUserInfoDto oneLoginUserInfoDto = spy(OneLoginUserInfoDto.builder()
                .emailAddress("email@example.com")
                .sub("sub")
                .build());

        @BeforeEach
        void setup() {
            reset(user);
            reset(oneLoginUserInfoDto);
        }

        @Test
        void emailUnchanged() {
            final NextStateArgs nextStateArgs = NextStateArgs.builder()
                    .oneLoginUserService(oneLoginUserService)
                    .user(user)
                    .jwt("jwt")
                    .logger(logger)
                    .hasAcceptedPrivacyPolicy(true)
                    .userInfo(oneLoginUserInfoDto)
                    .findAccountsMigrationEnabled("false")
                    .build();

            when(oneLoginUserService.hasEmailChanged(user, oneLoginUserInfoDto)).thenReturn(false);
            when(user.getLoginJourneyState()).thenReturn(LoginJourneyState.USER_MIGRATED_AND_READY);

            final LoginJourneyState nextState = state.nextState(nextStateArgs);

            assertFalse(oneLoginUserService.hasEmailChanged(user, oneLoginUserInfoDto));
            assertEquals(LoginJourneyState.USER_MIGRATED_AND_READY, nextState);
        }

        @Test
        void emailChanged() {
            final NextStateArgs nextStateArgs = NextStateArgs.builder()
                    .oneLoginUserService(oneLoginUserService)
                    .user(user)
                    .jwt("jwt")
                    .logger(logger)
                    .hasAcceptedPrivacyPolicy(true)
                    .userInfo(oneLoginUserInfoDto)
                    .findAccountsMigrationEnabled("false")
                    .build();

            when(oneLoginUserService.hasEmailChanged(user, oneLoginUserInfoDto)).thenReturn(true);
            when(user.getLoginJourneyState()).thenReturn(LoginJourneyState.USER_MIGRATED_AND_READY);

            final LoginJourneyState nextState = state.nextState(nextStateArgs);

            assertTrue(oneLoginUserService.hasEmailChanged(user, oneLoginUserInfoDto));
            assertEquals(LoginJourneyState.MIGRATING_FIND_EMAILS, nextState);
        }

        @Test
        void redirect_super_admin() {
            final LoginJourneyRedirect redirect = state.getLoginJourneyRedirect(RoleEnum.SUPER_ADMIN,null);
            assertEquals(LoginJourneyRedirect.SUPER_ADMIN_DASHBOARD, redirect);
        }

        @Test
        void redirect_super_admin_hasRedirectUrl() {
            final LoginJourneyRedirect redirect = state.getLoginJourneyRedirect(RoleEnum.SUPER_ADMIN,"http://localhost:3000/apply/admin/scheme/2/123456789");
            assertEquals(LoginJourneyRedirect.REDIRECT_URL_COOKIE, redirect);
        }

        @Test
        void redirect_admin() {
            final LoginJourneyRedirect redirect = state.getLoginJourneyRedirect(RoleEnum.ADMIN,null);
            assertEquals(LoginJourneyRedirect.ADMIN_DASHBOARD, redirect);
        }

        @Test
        void redirect_admin_hasRedirectUrl() {
            final LoginJourneyRedirect redirect = state.getLoginJourneyRedirect(RoleEnum.ADMIN,"http://localhost:3000/apply/admin/scheme/2/123456789");
            assertEquals(LoginJourneyRedirect.REDIRECT_URL_COOKIE, redirect);
        }

        @Test
        void redirect_technical_support() {
            final LoginJourneyRedirect redirect = state.getLoginJourneyRedirect(RoleEnum.TECHNICAL_SUPPORT,null);
            assertEquals(LoginJourneyRedirect.TECHNICAL_SUPPORT_DASHBOARD, redirect);
        }
        @Test
        void redirect_technical_support_hasRedirectUrl() {
            final LoginJourneyRedirect redirect = state.getLoginJourneyRedirect(RoleEnum.TECHNICAL_SUPPORT,"http://localhost:3000/apply/admin/scheme/2/123456789");
            assertEquals(LoginJourneyRedirect.TECHNICAL_SUPPORT_DASHBOARD, redirect);
        }
        @Test
        void redirect_applicant() {
            final LoginJourneyRedirect redirect = state.getLoginJourneyRedirect(RoleEnum.APPLICANT,null);
            assertEquals(LoginJourneyRedirect.REDIRECT_URL_COOKIE, redirect);
        }

        @Test
        void redirect_find() {
            final LoginJourneyRedirect redirect = state.getLoginJourneyRedirect(RoleEnum.FIND,null);
            assertEquals(LoginJourneyRedirect.REDIRECT_URL_COOKIE, redirect);
        }
    }

    @Nested
    class MigratingFindEmails {
        private final LoginJourneyState state = LoginJourneyState.MIGRATING_FIND_EMAILS;
        private final User user = spy(User.builder()
                .sub("sub")
                .loginJourneyState(state)
                .roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build()))
                .emailAddress("email@example.com")
                .build());
        private final OneLoginUserInfoDto oneLoginUserInfoDto = spy(OneLoginUserInfoDto.builder()
                .emailAddress("email@example.com")
                .sub("sub")
                .build());

        @BeforeEach
        void setup() {
            reset(user);
            reset(oneLoginUserInfoDto);
        }

        @Test
        void nextState() {
            final NextStateArgs nextStateArgs = NextStateArgs.builder()
                    .oneLoginUserService(oneLoginUserService)
                    .user(user)
                    .jwt("jwt")
                    .logger(logger)
                    .hasAcceptedPrivacyPolicy(true)
                    .userInfo(oneLoginUserInfoDto)
                    .findAccountsMigrationEnabled("true")
                    .build();

            final LoginJourneyState nextState = state.nextState(nextStateArgs);

            when(user.getLoginJourneyState()).thenReturn(LoginJourneyState.MIGRATING_FIND_EMAILS);
            verify(oneLoginUserService, times(1)).setUsersEmail(user, nextStateArgs.userInfo().getEmailAddress());
            assertEquals(LoginJourneyState.MIGRATING_FIND_EMAILS, nextState);
        }

        @Test
        void emailRedirectPage() {

            when(user.getLoginJourneyState()).thenReturn(LoginJourneyState.MIGRATING_FIND_EMAILS);
            LoginJourneyRedirect result = state.getLoginJourneyRedirect(null,null);

            assertEquals(LoginJourneyRedirect.EMAIL_UPDATED_PAGE, result);
        }
    }

    @Nested
    class UserReady {
        private final LoginJourneyState state = LoginJourneyState.USER_READY;
        private final User user = spy(User.builder()
                .sub("sub")
                .loginJourneyState(state)
                .roles(List.of(Role.builder().name(RoleEnum.APPLICANT).build()))
                .emailAddress("email@example.com")
                .build());
        private final OneLoginUserInfoDto oneLoginUserInfoDto = spy(OneLoginUserInfoDto.builder()
                .emailAddress("email@example.com")
                .sub("sub")
                .build());

        @BeforeEach
        void setup() {
            reset(user);
            reset(oneLoginUserInfoDto);
        }

        @Test
        void flagOn_HasColaSub() {
            final NextStateArgs nextStateArgs = NextStateArgs.builder()
                    .oneLoginUserService(oneLoginUserService)
                    .user(user)
                    .jwt("jwt")
                    .logger(logger)
                    .hasAcceptedPrivacyPolicy(true)
                    .userInfo(oneLoginUserInfoDto)
                    .findAccountsMigrationEnabled("true")
                    .build();

            when(user.hasColaSub()).thenReturn(true);
            when(user.getLoginJourneyState()).thenReturn(LoginJourneyState.USER_READY);

            final LoginJourneyState nextState = state.nextState(nextStateArgs);

            verify(oneLoginUserService, times(1)).setUsersApplyMigrationState(user, MigrationStatus.ALREADY_MIGRATED);
            verify(oneLoginUserService, never()).hasEmailChanged(user, nextStateArgs.userInfo());
            assertEquals(LoginJourneyState.MIGRATING_USER, nextState);
        }

        @Test
        void flagOn_NoColaSub() {
            final NextStateArgs nextStateArgs = NextStateArgs.builder()
                    .oneLoginUserService(oneLoginUserService)
                    .user(user)
                    .jwt("jwt")
                    .logger(logger)
                    .hasAcceptedPrivacyPolicy(true)
                    .userInfo(oneLoginUserInfoDto)
                    .findAccountsMigrationEnabled("true")
                    .build();

            when(user.hasColaSub()).thenReturn(false);
            when(user.getLoginJourneyState()).thenReturn(LoginJourneyState.USER_READY);

            final LoginJourneyState nextState = state.nextState(nextStateArgs);

            verify(oneLoginUserService, never()).setUsersApplyMigrationState(user, MigrationStatus.ALREADY_MIGRATED);
            verify(oneLoginUserService, never()).hasEmailChanged(user, nextStateArgs.userInfo());
            assertEquals(LoginJourneyState.MIGRATING_USER, nextState);
        }

        @Test
        void flagOff_emailsChanged() {
            final NextStateArgs nextStateArgs = NextStateArgs.builder()
                    .oneLoginUserService(oneLoginUserService)
                    .user(user)
                    .jwt("jwt")
                    .logger(logger)
                    .hasAcceptedPrivacyPolicy(true)
                    .userInfo(oneLoginUserInfoDto)
                    .findAccountsMigrationEnabled("false")
                    .build();

            when(oneLoginUserService.hasEmailChanged(user, oneLoginUserInfoDto)).thenReturn(true);
            when(user.getLoginJourneyState()).thenReturn(LoginJourneyState.USER_READY);

            final LoginJourneyState nextState = state.nextState(nextStateArgs);

            verify(oneLoginUserService, never()).setUsersApplyMigrationState(user, MigrationStatus.ALREADY_MIGRATED);
            verify(oneLoginUserService, times(1)).hasEmailChanged(user, nextStateArgs.userInfo());
            assertEquals(LoginJourneyState.MIGRATING_FIND_EMAILS, nextState);
        }

        @Test
        void flagOff_emailsUnchanged() {
            final NextStateArgs nextStateArgs = NextStateArgs.builder()
                    .oneLoginUserService(oneLoginUserService)
                    .user(user)
                    .jwt("jwt")
                    .logger(logger)
                    .hasAcceptedPrivacyPolicy(true)
                    .userInfo(oneLoginUserInfoDto)
                    .findAccountsMigrationEnabled("false")
                    .build();

            when(oneLoginUserService.hasEmailChanged(user, oneLoginUserInfoDto)).thenReturn(false);
            when(user.getLoginJourneyState()).thenReturn(LoginJourneyState.USER_READY);

            final LoginJourneyState nextState = state.nextState(nextStateArgs);

            verify(oneLoginUserService, never()).setUsersApplyMigrationState(user, MigrationStatus.ALREADY_MIGRATED);
            verify(oneLoginUserService, times(1)).hasEmailChanged(user, nextStateArgs.userInfo());
            assertEquals(LoginJourneyState.USER_READY, nextState);
        }


        @Test
        void redirect_super_admin() {
            final LoginJourneyRedirect redirect = state.getLoginJourneyRedirect(RoleEnum.SUPER_ADMIN,null);
            assertEquals(LoginJourneyRedirect.SUPER_ADMIN_DASHBOARD, redirect);
        }

        @Test
        void redirect_admin() {
            final LoginJourneyRedirect redirect = state.getLoginJourneyRedirect(RoleEnum.ADMIN,null);
            assertEquals(LoginJourneyRedirect.ADMIN_DASHBOARD, redirect);
        }

        @Test
        void redirect_technical_support() {
            final LoginJourneyRedirect redirect = state.getLoginJourneyRedirect(RoleEnum.TECHNICAL_SUPPORT,null);
            assertEquals(LoginJourneyRedirect.TECHNICAL_SUPPORT_DASHBOARD, redirect);
        }

        @Test
        void redirect_applicant() {
            final LoginJourneyRedirect redirect = state.getLoginJourneyRedirect(RoleEnum.APPLICANT,null);
            assertEquals(LoginJourneyRedirect.REDIRECT_URL_COOKIE, redirect);
        }

        @Test
        void redirect_find() {
            final LoginJourneyRedirect redirect = state.getLoginJourneyRedirect(RoleEnum.FIND,null);
            assertEquals(LoginJourneyRedirect.REDIRECT_URL_COOKIE, redirect);
        }
    }
}
