package gov.cabinetoffice.gapuserservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PrivacyPolicyDto {
    @NotBlank(message = "You need to agree to the privacy policy to continue.")
    private String acceptPrivacyPolicy;
}
