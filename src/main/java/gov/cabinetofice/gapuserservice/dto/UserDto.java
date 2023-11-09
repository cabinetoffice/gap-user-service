package gov.cabinetofice.gapuserservice.dto;

import gov.cabinetofice.gapuserservice.model.User;
import lombok.Builder;

import java.time.Instant;

@Builder
public record UserDto(String gapUserId, String emailAddress, String sub, Instant created) {
    public UserDto(User user) {
        this(user.getGapUserId().toString(), user.getEmailAddress(), user.getSub(), user.getCreated());
    }
}
