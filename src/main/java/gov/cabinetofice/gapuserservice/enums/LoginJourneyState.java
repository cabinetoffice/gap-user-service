package gov.cabinetofice.gapuserservice.enums;

import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.OneLoginService;

public enum LoginJourneyState {
    CREATING_NEW_USER {
        @Override
        public LoginJourneyState nextState(final OneLoginService oneLoginService, final User user) {
            super.nextState(oneLoginService, user);
            oneLoginService.createUser(user.getEmailAddress(), user.getSub());
            return PRIVACY_POLICY_PENDING;
        }

        @Override
        public LoginJourneyRedirect getRedirectUrl(final RoleEnum role) {
            return LoginJourneyRedirect.PRIVACY_POLICY_PAGE;
        }
    },

    PRIVACY_POLICY_PENDING {
        @Override
        public LoginJourneyState nextState(final OneLoginService oneLoginService, final User user) {
            super.nextState(oneLoginService, user);
            oneLoginService.setPrivacyPolicy(user.getSub());
            return PRIVACY_POLICY_ACCEPTED.nextState(oneLoginService, user);
        }
    },

    PRIVACY_POLICY_ACCEPTED {
        @Override
        public LoginJourneyState nextState(final OneLoginService oneLoginService, final User user) {
            super.nextState(oneLoginService, user);
            final RoleEnum roleEnum = user.getRole().getName();
            return switch (roleEnum) {
                case SUPER_ADMIN, ADMIN -> ADMIN_READY;
                case APPLICANT, FIND -> APPLICANT_READY;
            };
        }
    },

    ADMIN_READY {
        @Override
        public LoginJourneyRedirect getRedirectUrl(final RoleEnum role) {
            return switch (role) {
                case SUPER_ADMIN -> LoginJourneyRedirect.SUPER_ADMIN_DASHBOARD;
                case ADMIN -> LoginJourneyRedirect.ADMIN_DASHBOARD;
                default -> throw new RuntimeException("Invalid role");
            };
        }
    },

    APPLICANT_READY {
        @Override
        public LoginJourneyRedirect getRedirectUrl(final RoleEnum role) {
            return LoginJourneyRedirect.APPLICANT_APP;
        }
    };


    public LoginJourneyState nextState(final OneLoginService oneLoginService,
                                       final User user) {
        // TODO set state in the database
        return this;
    }

    public LoginJourneyRedirect getRedirectUrl(final RoleEnum role) {
        return null;
    }
}