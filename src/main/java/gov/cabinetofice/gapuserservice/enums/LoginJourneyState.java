package gov.cabinetofice.gapuserservice.enums;

import gov.cabinetofice.gapuserservice.model.RoleEnum;

import java.util.Objects;


public enum LoginJourneyState {
    PRIVACY_POLICY_PENDING {
        @Override
        public LoginJourneyState nextState(final NextStateArgs nextStateArgs) {
            nextStateArgs.logger().info("User: " + nextStateArgs.user().getSub() + " accepted the privacy policy");
            if (!nextStateArgs.hasAcceptedPrivacyPolicy()) return this;
            nextStateArgs.oneLoginUserService().setUsersLoginJourneyState(nextStateArgs.user(), LoginJourneyState.MIGRATING_USER);
            return MIGRATING_USER.nextState(nextStateArgs);
        }

        @Override
        public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
            return LoginJourneyRedirect.PRIVACY_POLICY_PAGE;
        }
    },

    MIGRATING_USER {
        @Override
        public LoginJourneyState nextState(final NextStateArgs nextStateArgs) {
            nextStateArgs.logger().info("Migrating user: " + nextStateArgs.user().getSub());
            if (Objects.equals(nextStateArgs.findAccountsMigrationEnabled(), "true")) {
                nextStateArgs.oneLoginUserService().migrateFindUser(nextStateArgs.user(), nextStateArgs.jwt());
            }
            if (nextStateArgs.user().hasColaSub() && nextStateArgs.user().getLoginJourneyState() != USER_READY) {
                nextStateArgs.oneLoginUserService().migrateApplyUser(nextStateArgs.user(), nextStateArgs.jwt());
            }

            if (Objects.equals(nextStateArgs.findAccountsMigrationEnabled(), "true")) {
                nextStateArgs.oneLoginUserService().setUsersLoginJourneyState(nextStateArgs.user(), USER_MIGRATED_AND_READY);
            } else {
                nextStateArgs.oneLoginUserService().setUsersLoginJourneyState(nextStateArgs.user(), USER_READY);
            }
            return this;
        }

        @Override
        public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
            return switch (role) {
                case SUPER_ADMIN -> LoginJourneyRedirect.SUPER_ADMIN_DASHBOARD;
                case TECHNICAL_SUPPORT ->  LoginJourneyRedirect.TECHNICAL_SUPPORT_DASHBOARD;
                case ADMIN -> LoginJourneyRedirect.ADMIN_MIGRATED;
                case APPLICANT, FIND -> LoginJourneyRedirect.APPLICANT_MIGRATED;
            };
        }
    },

    USER_MIGRATED_AND_READY {
        @Override
        public LoginJourneyState nextState(final NextStateArgs nextStateArgs) {
            if (nextStateArgs.userInfo() != null && nextStateArgs.oneLoginUserService().hasEmailChanged(nextStateArgs.user(), nextStateArgs.userInfo())) {
                return MIGRATING_FIND_EMAILS.nextState(nextStateArgs);
            }
            return this;
        }

        @Override
        public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
            return switch (role) {
                case SUPER_ADMIN -> LoginJourneyRedirect.SUPER_ADMIN_DASHBOARD;
                case TECHNICAL_SUPPORT ->  LoginJourneyRedirect.TECHNICAL_SUPPORT_DASHBOARD;
                case ADMIN -> LoginJourneyRedirect.ADMIN_DASHBOARD;
                case APPLICANT, FIND -> LoginJourneyRedirect.REDIRECT_URL_COOKIE;
            };
        }
    },

    MIGRATING_FIND_EMAILS {
        @Override
        public LoginJourneyState nextState(final NextStateArgs nextStateArgs) {
            nextStateArgs.oneLoginUserService().setUsersEmail(nextStateArgs.user(), nextStateArgs.userInfo().getEmailAddress());
            return this;
        }

        @Override
        public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
            return LoginJourneyRedirect.EMAIL_UPDATED_PAGE;
        }
    },

    USER_READY {
        @Override
        public LoginJourneyState nextState(final NextStateArgs nextStateArgs) {
            if (Objects.equals(nextStateArgs.findAccountsMigrationEnabled(), "true")) {
                if (nextStateArgs.user().hasColaSub()) {
                    nextStateArgs.oneLoginUserService().setUsersApplyMigrationState(nextStateArgs.user(), MigrationStatus.ALREADY_MIGRATED);
                }
                return MIGRATING_USER.nextState(nextStateArgs);
            } else {
                if (nextStateArgs.userInfo() != null && nextStateArgs.oneLoginUserService().hasEmailChanged(nextStateArgs.user(), nextStateArgs.userInfo())) {
                    return MIGRATING_FIND_EMAILS.nextState(nextStateArgs);
                }
                return this;
            }
        }
        @Override
        public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
            return switch (role) {
                case SUPER_ADMIN -> LoginJourneyRedirect.SUPER_ADMIN_DASHBOARD;
                case TECHNICAL_SUPPORT ->  LoginJourneyRedirect.TECHNICAL_SUPPORT_DASHBOARD;
                case ADMIN -> LoginJourneyRedirect.ADMIN_DASHBOARD;
                case APPLICANT, FIND -> LoginJourneyRedirect.REDIRECT_URL_COOKIE;
            };
        }
    };

    public abstract LoginJourneyState nextState(final NextStateArgs nextStateArgs);

    public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
        throw new UnsupportedOperationException("Error, make sure the enums next state function eventually ends up on a state that has a redirect URL");
    }
}