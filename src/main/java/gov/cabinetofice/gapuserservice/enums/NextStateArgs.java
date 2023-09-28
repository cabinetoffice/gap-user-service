package gov.cabinetofice.gapuserservice.enums;

import gov.cabinetofice.gapuserservice.dto.OneLoginUserInfoDto;
import gov.cabinetofice.gapuserservice.model.User;
import gov.cabinetofice.gapuserservice.service.user.OneLoginUserService;
import org.slf4j.Logger;
public record NextStateArgs(OneLoginUserService oneLoginUserService, User user, String jwt, Logger logger, boolean hasAcceptedPrivacyPolicy, OneLoginUserInfoDto userInfo, String findAccountsMigrationEnabled) {}