package gov.cabinetofice.gapuserservice.enums;

import static gov.cabinetofice.gapuserservice.web.LoginControllerV2.PRIVACY_POLICY_PAGE_VIEW;

public enum LoginJourneyRedirect {
    PRIVACY_POLICY_PAGE{
        @Override
        public String getRedirectUrl(String adminBaseUrl, String redirectUrlCookie) {
            return PRIVACY_POLICY_PAGE_VIEW;
        }
    },
    SUPER_ADMIN_DASHBOARD {
        @Override
        public String getRedirectUrl(String adminBaseUrl, String redirectUrlCookie) {
            return adminBaseUrl + "?redirectUrl=/super-admin-dashboard";
        }
    },
    ADMIN_DASHBOARD {
        @Override
        public String getRedirectUrl(String adminBaseUrl, String redirectUrlCookie) {
            return adminBaseUrl + "?redirectUrl=/dashboard";
        }
    },
    APPLICANT_APP {
        @Override
        public String getRedirectUrl(String adminBaseUrl, String redirectUrlCookie) {
            return redirectUrlCookie;
        }
    },

    MIGRATION_ERROR_PAGE {
        @Override
        public String getRedirectUrl(String adminBaseUrl, String redirectUrlCookie) {
            return adminBaseUrl + "/migration-error";
        }
    };

    public abstract String getRedirectUrl(String adminBaseUrl, String redirectUrlCookie);
}
