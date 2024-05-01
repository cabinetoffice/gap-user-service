package gov.cabinetoffice.gapuserservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class MigrateUserDto {
    private String oneLoginSub;
    private UUID colaSub;
}
