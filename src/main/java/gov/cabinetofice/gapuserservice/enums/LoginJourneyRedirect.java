package gov.cabinetofice.gapuserservice.enums;

import static gov.cabinetofice.gapuserservice.web.LoginControllerV2.PRIVACY_POLICY_PAGE_VIEW;

public enum LoginJourneyRedirect {
    PRIVACY_POLICY_PAGE,
    SUPER_ADMIN_DASHBOARD,
    ADMIN_DASHBOARD,
    APPLICANT_APP;

    public String getRedirectUrl(final String adminBaseUrl, final String redirectUrlCookie) {
        return switch (this) {
            case SUPER_ADMIN_DASHBOARD -> adminBaseUrl + "?redirectUrl=/super-admin-dashboard";
            case ADMIN_DASHBOARD -> adminBaseUrl + "?redirectUrl=/dashboard";
            case PRIVACY_POLICY_PAGE -> PRIVACY_POLICY_PAGE_VIEW;
            case APPLICANT_APP -> redirectUrlCookie;
        };
    }
}
