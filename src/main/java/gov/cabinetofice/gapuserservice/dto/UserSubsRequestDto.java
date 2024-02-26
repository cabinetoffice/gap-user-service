package gov.cabinetofice.gapuserservice.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record UserSubsRequestDto (List<String> userSubs) {

}