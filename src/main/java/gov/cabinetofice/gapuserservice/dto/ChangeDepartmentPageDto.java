package gov.cabinetofice.gapuserservice.dto;

import gov.cabinetofice.gapuserservice.model.User;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChangeDepartmentPageDto {
    private User user;
    private List<DepartmentDto> departments;
}
