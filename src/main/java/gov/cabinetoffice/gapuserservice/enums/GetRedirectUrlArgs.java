package gov.cabinetoffice.gapuserservice.enums;

import gov.cabinetoffice.gapuserservice.model.User;

public record GetRedirectUrlArgs(String adminBaseUrl, String applicantBaseUrl, String techSupportBaseUrl,
                                 String redirectUrlCookie, User user) {
}
