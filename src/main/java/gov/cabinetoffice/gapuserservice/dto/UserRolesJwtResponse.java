package gov.cabinetoffice.gapuserservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRolesJwtResponse {

    //need jsonProperty because Jackson removes the 'is' from 'isValid'
    @JsonProperty("isValid")
    private boolean isValid;
    @JsonProperty("isSuperAdmin")
    private boolean isSuperAdmin;
    @JsonProperty("isAdmin")
    private boolean isAdmin;
    @JsonProperty("isApplicant")
    private boolean isApplicant;
    @JsonProperty("isTechnicalSupport")
    private boolean isTechnicalSupport;
}
