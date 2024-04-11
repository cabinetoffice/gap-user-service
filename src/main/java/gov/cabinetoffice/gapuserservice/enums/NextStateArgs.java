package gov.cabinetoffice.gapuserservice.enums;

import gov.cabinetoffice.gapuserservice.dto.OneLoginUserInfoDto;
import gov.cabinetoffice.gapuserservice.model.User;
import gov.cabinetoffice.gapuserservice.service.user.OneLoginUserService;
import lombok.Builder;
import org.slf4j.Logger;

@Builder
public record NextStateArgs(OneLoginUserService oneLoginUserService, User user, String jwt, Logger logger,
                            boolean hasAcceptedPrivacyPolicy, OneLoginUserInfoDto userInfo,
                            String findAccountsMigrationEnabled) {
}