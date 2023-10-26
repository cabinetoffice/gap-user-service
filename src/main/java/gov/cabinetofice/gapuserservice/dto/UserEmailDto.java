package gov.cabinetofice.gapuserservice.dto;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class UserEmailDto {
    private byte[] emailAddress;
    private String sub;

    public UserEmailDto(byte[] emailAddress, String sub) {
        this.emailAddress = emailAddress;
        this.sub = sub;
    }
}