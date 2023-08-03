package gov.cabinetofice.gapuserservice.enums;

import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.OneLoginService;

public enum LoginJourneyState {
    PRIVACY_POLICY_PENDING {
        @Override
        public LoginJourneyState nextState(final OneLoginService oneLoginService, final User user) {
            oneLoginService.setUsersLoginJourneyState(user, PRIVACY_POLICY_ACCEPTED);
            return PRIVACY_POLICY_ACCEPTED.nextState(oneLoginService, user);
        }

        @Override
        public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
            return LoginJourneyRedirect.PRIVACY_POLICY_PAGE;
        }
    },

    PRIVACY_POLICY_ACCEPTED {
        @Override
        public LoginJourneyState nextState(final OneLoginService oneLoginService, final User user) {
            final LoginJourneyState nextState = user.hasColaSub() ? MIGRATING_USER : USER_READY;
            oneLoginService.setUsersLoginJourneyState(user, nextState);
            return nextState.nextState(oneLoginService, user);
        }
    },

    MIGRATING_USER {
        @Override
        public LoginJourneyState nextState(final OneLoginService oneLoginService, final User user) {
            LoginJourneyState nextState;
            try {
                oneLoginService.migrateUser(user);
                nextState = MIGRATION_SUCCEEDED;
            } catch (Exception e) {
                // TODO log error
                nextState = MIGRATION_FAILED;
            }
            oneLoginService.setUsersLoginJourneyState(user, nextState);
            return nextState.nextState(oneLoginService, user);
        }
    },

    MIGRATION_SUCCEEDED {
        @Override
        public LoginJourneyState nextState(OneLoginService oneLoginService, User user) {
            oneLoginService.setUsersLoginJourneyState(user, USER_READY);
            return this;
        }

        @Override
        public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
            return redirectToRelevantApp(role);
        }
    },

    MIGRATION_FAILED {
        @Override
        public LoginJourneyState nextState(OneLoginService oneLoginService, User user) {
            oneLoginService.setUsersLoginJourneyState(user, USER_READY);
            return this;
        }

        @Override
        public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
            return redirectToRelevantApp(role);
        }
    },

    USER_READY {
        @Override
        public LoginJourneyState nextState(OneLoginService oneLoginService, User user) {
            return this;
        }

        @Override
        public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
            return redirectToRelevantApp(role);
        }
    };

    private static LoginJourneyRedirect redirectToRelevantApp(final RoleEnum role) {
        return switch (role) {
            case SUPER_ADMIN -> LoginJourneyRedirect.SUPER_ADMIN_DASHBOARD;
            case ADMIN -> LoginJourneyRedirect.ADMIN_DASHBOARD;
            case APPLICANT, FIND -> LoginJourneyRedirect.APPLICANT_APP;
        };
    }

    public abstract LoginJourneyState nextState(final OneLoginService oneLoginService, final User user);

    public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
        throw new UnsupportedOperationException("Error, make sure the enums next state function eventually ends up on a state that has a redirect URL");
    }
}