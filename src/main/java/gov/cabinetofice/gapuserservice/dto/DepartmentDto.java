package gov.cabinetofice.gapuserservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DepartmentDto {
    private String id;
    private String name;
    private String type;
    private String ggisID;
}