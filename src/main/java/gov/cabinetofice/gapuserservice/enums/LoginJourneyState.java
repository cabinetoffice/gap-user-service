package gov.cabinetofice.gapuserservice.enums;

import gov.cabinetofice.gapuserservice.model.RoleEnum;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.OneLoginService;

public enum LoginJourneyState {
    CREATING_NEW_USER {
        @Override
        public LoginJourneyState nextState(final OneLoginService oneLoginService, final User user) {
            super.nextState(oneLoginService, user);
            // TODO create new user
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
            // TODO set acceptedPrivacyPolicy to true
            return PRIVACY_POLICY_ACCEPTED.nextState(oneLoginService, user);
        }
    },

    PRIVACY_POLICY_ACCEPTED {
        @Override
        public LoginJourneyState nextState(final OneLoginService oneLoginService, final User user) {
            super.nextState(oneLoginService, user);
            // TODO fetch role
            // TODO check if user has matching email
            // TODO return appropriate next state
            return PRIVACY_POLICY_ACCEPTED.nextState(oneLoginService, user);
        }
    },

    ADMIN_READY {
        @Override
        public LoginJourneyRedirect getRedirectUrl(final RoleEnum role) {
            if (role == RoleEnum.SUPER_ADMIN) return LoginJourneyRedirect.SUPER_ADMIN_DASHBOARD;
            if (role == RoleEnum.ADMIN) return LoginJourneyRedirect.ADMIN_DASHBOARD;
            throw new RuntimeException("Invalid role");
        }
    },

    APPLICANT_READY {
        @Override
        public LoginJourneyRedirect getRedirectUrl(final RoleEnum role) {
            return LoginJourneyRedirect.APPLICANT_APP;
        }
    };


    public LoginJourneyState nextState(final OneLoginService oneLoginService, final User user) {
        // TODO set state in the database
        return this;
    }

    public LoginJourneyRedirect getRedirectUrl(final RoleEnum role) {
        return null;
    }
}