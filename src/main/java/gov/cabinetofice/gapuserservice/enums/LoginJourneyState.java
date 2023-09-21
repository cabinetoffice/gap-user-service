package gov.cabinetofice.gapuserservice.enums;

import gov.cabinetofice.gapuserservice.model.RoleEnum;

public enum LoginJourneyState {
    PRIVACY_POLICY_PENDING {
        @Override
        public LoginJourneyState nextState(final NextStateArgs nextStateArgs) {
            nextStateArgs.logger().debug("User: " + nextStateArgs.user().getSub() + " accepted the privacy policy");
            if (!nextStateArgs.hasAcceptedPrivacyPolicy()) return this;
            nextStateArgs.oneLoginService().setUsersLoginJourneyState(nextStateArgs.user(), MIGRATING_USER);
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
            nextStateArgs.logger().debug("Migrating user: " + nextStateArgs.user().getSub());
            LoginJourneyState nextState;
            try {
                if (nextStateArgs.user().hasColaSub()) {
                    nextStateArgs.oneLoginService().migrateUser(nextStateArgs.user(), nextStateArgs.jwt());
                    nextStateArgs.logger().info("Successfully migrated apply user: " + nextStateArgs.user().getSub());
                }
                nextStateArgs.oneLoginService().migrateFindUser(nextStateArgs.userInfo().getEmailAddress(), nextStateArgs.userInfo().getSub(), nextStateArgs.jwt());
                nextState = MIGRATION_SUCCEEDED;
                nextStateArgs.logger().info("Successfully migrated find user: " + nextStateArgs.user().getSub());
            } catch (Exception e) {
                nextState = MIGRATION_FAILED;
                nextStateArgs.logger().error("Failed to migrate user: " + nextStateArgs.user().getSub(), e);
            }

            nextStateArgs.oneLoginService().setUsersLoginJourneyState(nextStateArgs.user(), nextState);
            return nextState.nextState(nextStateArgs);
        }
    },

    MIGRATION_SUCCEEDED {
        @Override
        public LoginJourneyState nextState(final NextStateArgs nextStateArgs) {
            nextStateArgs.oneLoginService().setUsersLoginJourneyState(nextStateArgs.user(), USER_READY_FOR_REAL);
            return this;
        }

        @Override
        public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
            return switch (role) {
                case SUPER_ADMIN -> LoginJourneyRedirect.SUPER_ADMIN_DASHBOARD;
                case TECHNICAL_SUPPORT ->  LoginJourneyRedirect.TECHNICAL_SUPPORT_DASHBOARD;
                case ADMIN -> LoginJourneyRedirect.ADMIN_DASHBOARD_MIGRATION_PASS;
                case APPLICANT, FIND -> LoginJourneyRedirect.APPLICANT_APP_MIGRATION_PASS;
            };
        }
    },

    MIGRATION_FAILED {
        @Override
        public LoginJourneyState nextState(final NextStateArgs nextStateArgs) {
            nextStateArgs.oneLoginService().setUsersLoginJourneyState(nextStateArgs.user(), USER_READY_FOR_REAL);
            return this;
        }

        @Override
        public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
            return switch (role) {
                case SUPER_ADMIN -> LoginJourneyRedirect.SUPER_ADMIN_DASHBOARD;
                case TECHNICAL_SUPPORT ->  LoginJourneyRedirect.TECHNICAL_SUPPORT_DASHBOARD;
                case ADMIN -> LoginJourneyRedirect.ADMIN_DASHBOARD_MIGRATION_FAIL;
                case APPLICANT, FIND -> LoginJourneyRedirect.APPLICANT_APP_MIGRATION_FAIL;
            };
        }
    },

    USER_READY {
        @Override
        public LoginJourneyState nextState(final NextStateArgs nextStateArgs) {
            nextStateArgs.logger().debug("Migrating Find user: " + nextStateArgs.userInfo().getSub());
            LoginJourneyState nextState;
            try {
                nextStateArgs.oneLoginService().migrateFindUser(nextStateArgs.userInfo().getEmailAddress(), nextStateArgs.userInfo().getSub(), nextStateArgs.jwt());
                nextState = FIND_MIGRATION_SUCCEEDED;
                nextStateArgs.logger().info("Successfully migrated Find user: " + nextStateArgs.userInfo().getSub());
            } catch (Exception e) {
                nextState = FIND_MIGRATION_FAILED;
                nextStateArgs.logger().error("Failed to migrate Find user: " + nextStateArgs.userInfo().getSub(), e);
            }

            nextStateArgs.oneLoginService().setUsersLoginJourneyState(nextStateArgs.user(), nextState);
            return nextState.nextState(nextStateArgs);
        }
    },

    FIND_MIGRATION_SUCCEEDED {
        @Override
        public LoginJourneyState nextState(final NextStateArgs nextStateArgs) {
            nextStateArgs.oneLoginService().setUsersLoginJourneyState(nextStateArgs.user(), USER_READY_FOR_REAL);
            return this;
        }

        @Override
        public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
            return switch (role) {
                case SUPER_ADMIN -> LoginJourneyRedirect.SUPER_ADMIN_DASHBOARD;
                case TECHNICAL_SUPPORT ->  LoginJourneyRedirect.TECHNICAL_SUPPORT_DASHBOARD;
                case ADMIN -> LoginJourneyRedirect.ADMIN_DASHBOARD_MIGRATION_PASS;
                case APPLICANT, FIND -> LoginJourneyRedirect.APPLICANT_APP_MIGRATION_PASS;
            };
        }
    },

    FIND_MIGRATION_FAILED {
        @Override
        public LoginJourneyState nextState(final NextStateArgs nextStateArgs) {
            nextStateArgs.oneLoginService().setUsersLoginJourneyState(nextStateArgs.user(), USER_READY_FOR_REAL);
            return this;
        }

        @Override
        public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
            return switch (role) {
                case SUPER_ADMIN -> LoginJourneyRedirect.SUPER_ADMIN_DASHBOARD;
                case TECHNICAL_SUPPORT ->  LoginJourneyRedirect.TECHNICAL_SUPPORT_DASHBOARD;
                case ADMIN -> LoginJourneyRedirect.ADMIN_DASHBOARD_MIGRATION_FAIL;
                case APPLICANT, FIND -> LoginJourneyRedirect.APPLICANT_APP_MIGRATION_FAIL;
            };
        }
    },

    USER_READY_FOR_REAL {
        @Override
        public LoginJourneyState nextState(final NextStateArgs nextStateArgs) {
            nextStateArgs.oneLoginService().setUsersLoginJourneyState(nextStateArgs.user(), this);
            if (nextStateArgs.userInfo() != null) {
                final String newEmail = nextStateArgs.userInfo().getEmailAddress();
                final String oldEmail = nextStateArgs.user().getEmailAddress();
                final boolean emailsDiffer = !newEmail.equals(oldEmail);

                if (emailsDiffer) {
                    nextStateArgs.oneLoginService().setUsersEmail(nextStateArgs.user(), newEmail);
                    nextStateArgs.oneLoginService().setUsersLoginJourneyState(nextStateArgs.user(), MIGRATING_FIND_EMAILS);
                    return MIGRATING_FIND_EMAILS.nextState(nextStateArgs);
                }
            }
            return this;
        }


        @Override
        public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
            return switch (role) {
                case SUPER_ADMIN -> LoginJourneyRedirect.SUPER_ADMIN_DASHBOARD;
                case TECHNICAL_SUPPORT ->  LoginJourneyRedirect.TECHNICAL_SUPPORT_DASHBOARD;
                case ADMIN -> LoginJourneyRedirect.ADMIN_DASHBOARD;
                case APPLICANT, FIND -> LoginJourneyRedirect.APPLICANT_APP;
            };
        }
    },

    MIGRATING_FIND_EMAILS {
        @Override
        public LoginJourneyState nextState(final NextStateArgs nextStateArgs) {
            nextStateArgs.oneLoginService().setUsersLoginJourneyState(nextStateArgs.user(), USER_READY_FOR_REAL);
            return this;
        }

        @Override
        public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
            return LoginJourneyRedirect.EMAIL_UPDATED_PAGE;
        }
    };

    public abstract LoginJourneyState nextState(final NextStateArgs nextStateArgs);

    public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
        throw new UnsupportedOperationException("Error, make sure the enums next state function eventually ends up on a state that has a redirect URL");
    }
}