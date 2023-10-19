package gov.cabinetofice.gapuserservice.dto;

import lombok.Data;


@Data
public class UserEmailDto {
    private byte[] emailAddress;
    private String sub;

    public UserEmailDto(byte[] emailAddress, String sub) {
        this.emailAddress = emailAddress;
        this.sub = sub;
    }
}