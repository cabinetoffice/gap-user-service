package gov.cabinetoffice.gapuserservice.dto;

import gov.cabinetoffice.gapuserservice.model.User;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChangeDepartmentPageDto {
    private User user;
    private List<DepartmentDto> departments;
}
