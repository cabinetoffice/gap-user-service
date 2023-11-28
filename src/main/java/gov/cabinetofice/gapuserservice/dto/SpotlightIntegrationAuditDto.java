package gov.cabinetofice.gapuserservice.dto;

import gov.cabinetofice.gapuserservice.enums.SpotlightOAuthAuditEvent;
import gov.cabinetofice.gapuserservice.enums.SpotlightOAuthAuditStatus;

import java.util.Date;

    public record SpotlightIntegrationAuditDto(
                                          String name,
                                          Integer id,
                                          SpotlightOAuthAuditEvent event,
                                          SpotlightOAuthAuditStatus status,
                                          Date timestamp) {}

