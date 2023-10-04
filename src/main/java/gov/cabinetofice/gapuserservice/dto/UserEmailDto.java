package gov.cabinetofice.gapuserservice.dto;

import lombok.Data;


@Data
public class UserEmailDto {
    private String emailAddress;
    private String sub;

    public UserEmailDto(String emailAddress, String sub) {
        this.emailAddress = emailAddress;
        this.sub = sub;
    }
}
