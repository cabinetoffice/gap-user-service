package gov.cabinetofice.gapuserservice.enums;

import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.OneLoginService;
import org.apache.logging.log4j.Logger;

public enum LoginJourneyState {
    PRIVACY_POLICY_PENDING {
        @Override
        public LoginJourneyState nextState(final OneLoginService oneLoginService,
                                           final User user,
                                           final String jwt,
                                           final Logger logger) {
            logger.debug("User: " + user.getSub() + " accepted the privacy policy");
            oneLoginService.setUsersLoginJourneyState(user, PRIVACY_POLICY_ACCEPTED);
            return PRIVACY_POLICY_ACCEPTED.nextState(oneLoginService, user, jwt, logger);
        }

        @Override
        public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
            return LoginJourneyRedirect.PRIVACY_POLICY_PAGE;
        }
    },

    PRIVACY_POLICY_ACCEPTED {
        @Override
        public LoginJourneyState nextState(final OneLoginService oneLoginService,
                                           final User user,
                                           final String jwt,
                                           final Logger logger) {
            final LoginJourneyState nextState = user.hasColaSub() ? MIGRATING_USER : USER_READY;
            oneLoginService.setUsersLoginJourneyState(user, nextState);
            return nextState.nextState(oneLoginService, user, jwt, logger);
        }
    },

    MIGRATING_USER {
        @Override
        public LoginJourneyState nextState(final OneLoginService oneLoginService,
                                           final User user,
                                           final String jwt,
                                           final Logger logger) {
            logger.debug("Migrating user: " + user.getSub());
            LoginJourneyState nextState;
            try {
                oneLoginService.migrateUser(user, jwt);
                nextState = MIGRATION_SUCCEEDED;
                logger.info("Successfully migrated user: " + user.getSub());
            } catch (Exception e) {
                nextState = MIGRATION_FAILED;
                logger.error("Failed to migrate user: " + user.getSub(), e);
            }

            oneLoginService.setUsersLoginJourneyState(user, nextState);
            return nextState.nextState(oneLoginService, user, jwt, logger);
        }
    },

    MIGRATION_SUCCEEDED {
        @Override
        public LoginJourneyState nextState(final OneLoginService oneLoginService,
                                           final User user,
                                           final String jwt,
                                           final Logger logger) {
            oneLoginService.setUsersLoginJourneyState(user, USER_READY);
            return this;
        }

        @Override
        public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
            return switch (role) {
                case SUPER_ADMIN -> LoginJourneyRedirect.SUPER_ADMIN_DASHBOARD;
                case ADMIN -> LoginJourneyRedirect.ADMIN_DASHBOARD_MIGRATION_PASS;
                case APPLICANT, FIND -> LoginJourneyRedirect.APPLICANT_APP_MIGRATION_PASS;
            };
        }
    },

    MIGRATION_FAILED {
        @Override
        public LoginJourneyState nextState(final OneLoginService oneLoginService,
                                           final User user,
                                           final String jwt,
                                           final Logger logger) {
            oneLoginService.setUsersLoginJourneyState(user, USER_READY);
            return this;
        }

        @Override
        public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
            return switch (role) {
                case SUPER_ADMIN -> LoginJourneyRedirect.SUPER_ADMIN_DASHBOARD;
                case ADMIN -> LoginJourneyRedirect.ADMIN_DASHBOARD_MIGRATION_FAIL;
                case APPLICANT, FIND -> LoginJourneyRedirect.APPLICANT_APP_MIGRATION_FAIL;
            };
        }
    },

    USER_READY {
        @Override
        public LoginJourneyState nextState(final OneLoginService oneLoginService,
                                           final User user,
                                           final String jwt,
                                           final Logger logger) {
            return this;
        }

        @Override
        public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
            return switch (role) {
                case SUPER_ADMIN -> LoginJourneyRedirect.SUPER_ADMIN_DASHBOARD;
                case ADMIN -> LoginJourneyRedirect.ADMIN_DASHBOARD;
                case APPLICANT, FIND -> LoginJourneyRedirect.APPLICANT_APP;
            };
        }
    };

    public abstract LoginJourneyState nextState(final OneLoginService oneLoginService, final User user, final String jwt, final Logger logger);

    public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
        throw new UnsupportedOperationException("Error, make sure the enums next state function eventually ends up on a state that has a redirect URL");
    }
}