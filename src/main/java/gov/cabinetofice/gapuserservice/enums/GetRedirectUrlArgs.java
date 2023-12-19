package gov.cabinetofice.gapuserservice.enums;

import gov.cabinetofice.gapuserservice.model.User;

public record GetRedirectUrlArgs(String adminBaseUrl, String applicantBaseUrl, String techSupportBaseUrl,
                                 String redirectUrlCookie, User user) {
}
