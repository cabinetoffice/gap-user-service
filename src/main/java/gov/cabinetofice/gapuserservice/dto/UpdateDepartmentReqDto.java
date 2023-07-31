package gov.cabinetofice.gapuserservice.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class UpdateDepartmentReqDto {
    @NotBlank(message = "Enter a department name.")
    private String departmentName;

    @NotBlank(message = "Enter a ggis id")
    String ggisId;
}
