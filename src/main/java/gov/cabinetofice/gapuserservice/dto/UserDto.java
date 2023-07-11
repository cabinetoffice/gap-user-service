package gov.cabinetofice.gapuserservice.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserDto {
    private String gap_user_id;
    private String email;
    private String sub;
}
