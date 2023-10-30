package gov.cabinetofice.gapuserservice.enums;

import gov.cabinetofice.gapuserservice.util.WebUtil;

import java.util.Arrays;
import java.util.List;

import static gov.cabinetofice.gapuserservice.web.LoginControllerV2.PRIVACY_POLICY_PAGE_VIEW;
import static gov.cabinetofice.gapuserservice.web.LoginControllerV2.UPDATED_EMAIL_PAGE_VIEW;

public enum LoginJourneyRedirect {
    PRIVACY_POLICY_PAGE {
        @Override
        public String getRedirectUrl(GetRedirectUrlArgs getRedirectUrlArgs) {
            return PRIVACY_POLICY_PAGE_VIEW;
        }
    },

    EMAIL_UPDATED_PAGE {
        @Override
        public String getRedirectUrl(GetRedirectUrlArgs getRedirectUrlArgs) {
            return UPDATED_EMAIL_PAGE_VIEW;
        }
    },

    SUPER_ADMIN_DASHBOARD {
        @Override
        public String getRedirectUrl(GetRedirectUrlArgs getRedirectUrlArgs) {
            return getRedirectUrlArgs.adminBaseUrl() + "?redirectUrl=/super-admin-dashboard";
        }
    },

    ADMIN_DASHBOARD {
        @Override
        public String getRedirectUrl(GetRedirectUrlArgs getRedirectUrlArgs) {
            return getRedirectUrlArgs.adminBaseUrl() + "?redirectUrl=/dashboard";
        }
    },

    REDIRECT_URL_COOKIE {
        @Override
        public String getRedirectUrl(GetRedirectUrlArgs getRedirectUrlArgs) {
            return getRedirectUrlArgs.redirectUrlCookie();
        }
    },

    APPLICANT_MIGRATED {
        @Override
        public String getRedirectUrl(GetRedirectUrlArgs getRedirectUrlArgs) {
            final MigrationStatus applyMigrationStatus = getRedirectUrlArgs.user().getApplyAccountMigrated();
            final MigrationStatus findMigrationStatus = getRedirectUrlArgs.user().getFindAccountMigrated();
            List<String> queryParams = Arrays.asList(
                    "applyMigrationStatus=" + applyMigrationStatus,
                    "findMigrationStatus=" + findMigrationStatus);

            return WebUtil.parseUrlRequestParameters(getRedirectUrlArgs.redirectUrlCookie(), queryParams);
        }
    },

    ADMIN_MIGRATED {
        @Override
        public String getRedirectUrl(GetRedirectUrlArgs getRedirectUrlArgs) {
            final MigrationStatus applyMigrationStatus = getRedirectUrlArgs.user().getApplyAccountMigrated();
            final MigrationStatus findMigrationStatus = getRedirectUrlArgs.user().getFindAccountMigrated();
            return ADMIN_DASHBOARD.getRedirectUrl(getRedirectUrlArgs) + "&applyMigrationStatus=" + applyMigrationStatus + "&findMigrationStatus=" + findMigrationStatus;
        }
    },

    TECHNICAL_SUPPORT_DASHBOARD {
        @Override
        public String getRedirectUrl(GetRedirectUrlArgs getRedirectUrlArgs) {
            return getRedirectUrlArgs.techSupportBaseUrl();
        }
    };

    public abstract String getRedirectUrl(GetRedirectUrlArgs getRedirectUrlArgs);
}
