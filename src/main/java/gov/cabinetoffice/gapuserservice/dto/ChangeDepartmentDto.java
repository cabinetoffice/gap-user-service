package gov.cabinetoffice.gapuserservice.dto;

import gov.cabinetoffice.gapuserservice.validation.annotations.FieldNotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class ChangeDepartmentDto {
    @FieldNotNull(message = "Select a department")
    private Integer departmentId;

    public ChangeDepartmentDto(Integer departmentId) {
        this.departmentId = departmentId;
    }
}
