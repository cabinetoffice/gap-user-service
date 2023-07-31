package gov.cabinetofice.gapuserservice.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class DepartmentReqDto {
    @Size(max = 255, message = "Department name cannot exceed 255 characters")
    @NotBlank(message = "Enter a Department name.")
    private String name;

    @Size(max = 255, message = "GGIS ID cannot exceed 255 characters")
    @NotBlank(message = "Enter a GGIS ID")
    private String ggisID;
}
