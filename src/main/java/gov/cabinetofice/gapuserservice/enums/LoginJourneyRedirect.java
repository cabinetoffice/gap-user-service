package gov.cabinetofice.gapuserservice.enums;

import gov.cabinetofice.gapuserservice.util.EncodeURIComponent;
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
            return buildAdminRedirectionUrl(getRedirectUrlArgs, true);
        }
    },

    ADMIN_DASHBOARD {
        @Override
        public String getRedirectUrl(GetRedirectUrlArgs getRedirectUrlArgs) {
            return buildAdminRedirectionUrl(getRedirectUrlArgs, false);
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
            String encodedRedirectUrl = EncodeURIComponent.encodeURI("/dashboard?" +
                    "applyMigrationStatus=" + applyMigrationStatus +
                    "&findMigrationStatus=" + findMigrationStatus
            );
            List<String> queryParams = List.of("redirectUrl=" + encodedRedirectUrl);
            return WebUtil.parseUrlRequestParameters(getRedirectUrlArgs.adminBaseUrl(), queryParams);
        }
    },

    TECHNICAL_SUPPORT_DASHBOARD {
        @Override
        public String getRedirectUrl(GetRedirectUrlArgs getRedirectUrlArgs) {
            return getRedirectUrlArgs.techSupportBaseUrl();
        }
    };

    private static String buildAdminRedirectionUrl(GetRedirectUrlArgs getRedirectUrlArgs, boolean isSuperAdmin) {
        final String adminRedirectionBasePath = getRedirectUrlArgs.adminBaseUrl() + "?redirectUrl=";
        String redirectUrl = isSuperAdmin ? "/super-admin-dashboard" : "/dashboard";

        if(getRedirectUrlArgs.redirectUrlCookie()!= null){
            redirectUrl=  getRedirectUrlArgs.redirectUrlCookie().replace(getRedirectUrlArgs.adminBaseUrl(),"");
        }

        return adminRedirectionBasePath + redirectUrl;
    }

    public abstract String getRedirectUrl(GetRedirectUrlArgs getRedirectUrlArgs);
}
