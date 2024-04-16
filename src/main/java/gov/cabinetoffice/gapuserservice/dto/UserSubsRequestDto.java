package gov.cabinetoffice.gapuserservice.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record UserSubsRequestDto (List<String> userSubs) {

}