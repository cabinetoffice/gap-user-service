package gov.cabinetofice.gapuserservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleDto {
    private Integer id;
    private String name;
    private String description;
    private String label;
}