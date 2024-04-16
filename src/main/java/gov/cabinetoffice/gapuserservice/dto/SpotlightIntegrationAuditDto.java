package gov.cabinetoffice.gapuserservice.dto;

import gov.cabinetoffice.gapuserservice.enums.SpotlightOAuthAuditEvent;
import gov.cabinetoffice.gapuserservice.enums.SpotlightOAuthAuditStatus;

import java.util.Date;

public record SpotlightIntegrationAuditDto(
        String name,
        Integer id,
        SpotlightOAuthAuditEvent event,
        SpotlightOAuthAuditStatus status,
        Date timestamp) {
}

