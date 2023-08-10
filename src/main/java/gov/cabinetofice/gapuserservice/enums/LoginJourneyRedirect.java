package gov.cabinetofice.gapuserservice.enums;

import static gov.cabinetofice.gapuserservice.web.LoginControllerV2.PRIVACY_POLICY_PAGE_VIEW;

public enum LoginJourneyRedirect {
    PRIVACY_POLICY_PAGE {
        @Override
        public String getRedirectUrl(String adminBaseUrl, String applicantBaseUrl, String redirectUrlCookie) {
            return PRIVACY_POLICY_PAGE_VIEW;
        }
    },
    SUPER_ADMIN_DASHBOARD {
        @Override
        public String getRedirectUrl(String adminBaseUrl, String applicantBaseUrl, String redirectUrlCookie) {
            return adminBaseUrl + "?redirectUrl=/super-admin-dashboard";
        }
    },
    ADMIN_DASHBOARD {
        @Override
        public String getRedirectUrl(String adminBaseUrl, String applicantBaseUrl, String redirectUrlCookie) {
            return adminBaseUrl + "?redirectUrl=/dashboard";
        }
    },
    APPLICANT_APP {
        @Override
        public String getRedirectUrl(String adminBaseUrl, String applicantBaseUrl, String redirectUrlCookie) {
            return redirectUrlCookie;
        }
    },

    APPLICANT_APP_MIGRATION_PASS {
        @Override
        public String getRedirectUrl(String adminBaseUrl, String applicantBaseUrl, String redirectUrlCookie) {
            return APPLICANT_APP.getRedirectUrl(adminBaseUrl, applicantBaseUrl, redirectUrlCookie) + "?migrationStatus=success";
        }
    },

    APPLICANT_APP_MIGRATION_FAIL {
        @Override
        public String getRedirectUrl(String adminBaseUrl, String applicantBaseUrl, String redirectUrlCookie) {
            return applicantBaseUrl + "/dashboard?migrationStatus=error";
        }
    },

    ADMIN_DASHBOARD_MIGRATION_PASS {
        @Override
        public String getRedirectUrl(String adminBaseUrl, String applicantBaseUrl, String redirectUrlCookie) {
            return ADMIN_DASHBOARD.getRedirectUrl(adminBaseUrl, applicantBaseUrl, redirectUrlCookie) + "?migrationStatus=success";
        }
    },

    ADMIN_DASHBOARD_MIGRATION_FAIL {
        @Override
        public String getRedirectUrl(String adminBaseUrl, String applicantBaseUrl, String redirectUrlCookie) {
            return ADMIN_DASHBOARD.getRedirectUrl(adminBaseUrl, applicantBaseUrl, redirectUrlCookie) + "?migrationStatus=error";
        }
    };

    public abstract String getRedirectUrl(String adminBaseUrl, String applicantBaseUrl, String redirectUrlCookie);
}
