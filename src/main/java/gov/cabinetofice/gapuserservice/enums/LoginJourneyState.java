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
            return USER_READY.nextState(oneLoginService, user);
        }
    },

    USER_READY {
        @Override
        public LoginJourneyState nextState(OneLoginService oneLoginService, User user) {
            oneLoginService.setUsersLoginJourneyState(user, this);
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

    public abstract LoginJourneyState nextState(final OneLoginService oneLoginService, final User user);

    public LoginJourneyRedirect getLoginJourneyRedirect(final RoleEnum role) {
        throw new UnsupportedOperationException("Error, make sure the enums next state function eventually ends up on a state that has a redirect URL");
    }
}